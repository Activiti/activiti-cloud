/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.acc.core.steps.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.WebsocketSender;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.test.StepVerifier;

@EnableRuntimeFeignContext
public class NotificationsSteps {

    private static final String GRAPHQL_WS = "graphql-ws";
    private static final String AUTHORIZATION = "Authorization";
    private static final Duration TIMEOUT = Duration.ofMillis(90000);
    private static final WebsocketClientSpec graphqlWsClientSpec = WebsocketClientSpec
        .builder()
        .protocols(GRAPHQL_WS)
        .build();

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private RuntimeTestsConfigurationProperties properties;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    public String getRuntimeBundleServiceName() {
        return properties.getRuntimeBundleServiceName();
    }

    @SuppressWarnings({ "serial" })
    @Step
    public ReplayProcessor<String> subscribe(
        String accessToken,
        String query,
        Map<String, Object> variables,
        Consumer<Subscription> action
    ) throws InterruptedException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        WebsocketSender client = HttpClient
            .create()
            .wiretap(true)
            .headers(h -> h.add(AUTHORIZATION, "Bearer " + accessToken))
            .websocket(graphqlWsClientSpec)
            .uri(properties.getGraphqlWsUrl());

        Map<String, Object> json = new LinkedHashMap<String, Object>() {
            {
                put("type", "start");
                put("id", "1");
                put(
                    "payload",
                    new LinkedHashMap<String, Object>() {
                        {
                            put("query", query);
                            put("variables", variables);
                        }
                    }
                );
            }
        };

        String startMessage;
        try {
            startMessage = objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // handle start subscription
        client
            .handle((i, o) -> {
                o.sendString(Mono.just(startMessage)).then().log("send").subscribe();

                return i
                    .aggregateFrames()
                    .receive()
                    .asString()
                    .log("receive")
                    .subscribeWith(data)
                    .doOnCancel(() -> {
                        // Let's close websocket and complete data processor
                        o.sendClose().doOnTerminate(data::onComplete).block(Duration.ofSeconds(2));
                    })
                    .doOnSubscribe(action);
            })
            .log("handle")
            .subscribe();

        return data;
    }

    @Step
    public void verifyData(ReplayProcessor<String> data, String... messages) {
        StepVerifier.create(data).expectNext(messages).expectComplete().verify(TIMEOUT);
    }
}
