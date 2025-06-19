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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.services.test.identity.JwtGraphQlClientInterceptor;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.test.tester.WebSocketGraphQlTester;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;

@EnableRuntimeFeignContext
public class NotificationsSteps {

    @Autowired
    private RuntimeTestsConfigurationProperties properties;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    public String getRuntimeBundleServiceName() {
        return properties.getRuntimeBundleServiceName();
    }

    @SuppressWarnings({ "serial" })
    @Step
    public Flux<List> subscribe(
        ReplayProcessor<List> processor,
        String accessToken,
        String query,
        Map<String, Object> variables,
        Consumer<Subscription> action
    ) throws URISyntaxException {
        URI url = new URI(properties.getGraphqlWsUrl());
        WebSocketGraphQlTester graphQlTester = WebSocketGraphQlTester
            .builder(url, new ReactorNettyWebSocketClient())
            .interceptor(new JwtGraphQlClientInterceptor(accessToken))
            .build();

        return graphQlTester
            .document(query)
            .variables(variables)
            .executeSubscription()
            .toFlux("engineEvents", List.class)
            .doOnSubscribe(action)
            .doOnCancel(()-> {
                processor.onComplete();
            })
            .doOnComplete(()-> {
                processor.onComplete();
            })
            .doFinally(signal ->
                processor.onComplete());
    }
}
