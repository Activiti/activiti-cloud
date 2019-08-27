package org.activiti.cloud.acc.core.steps.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessRuntimeService;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClient.WebsocketSender;
import reactor.test.StepVerifier;

@EnableRuntimeFeignContext
public class NotificationsSteps {

    private static final String GRAPHQL_WS = "graphql-ws";
    private static final String AUTHORIZATION = "Authorization";
    private static final Duration TIMEOUT = Duration.ofMillis(90000);

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private RuntimeTestsConfigurationProperties properties;

    @Autowired
    private ProcessRuntimeService processRuntimeService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Step
    public void checkServicesHealth() {
        assertThat(processRuntimeService.isServiceUp()).isTrue();
    }
    
    public String getRuntimeBundleServiceName() {
        return properties.getRuntimeBundleServiceName();
    }

    @SuppressWarnings({"serial"})
    @Step
    public ReplayProcessor<String> subscribe(String accessToken, 
                                             String query, 
                                             Map<String,Object> variables, 
                                             Consumer<Subscription> action) throws InterruptedException {
        ReplayProcessor<String> data = ReplayProcessor.create();

        WebsocketSender client = HttpClient.create()
                .wiretap(true)
                .headers(h -> h.add(AUTHORIZATION, "Bearer " + accessToken))
                .websocket(GRAPHQL_WS)
                .uri(properties.getGraphqlWsUrl());
        
        Map<String, Object> json = new LinkedHashMap<String, Object>() {{
            put("type", "start");
            put("id", "1");
            put("payload", new LinkedHashMap<String, Object>() {{
                put("query", query);
                put("variables", variables);
            }});
        }};

        String startMessage;
        try {
            startMessage = objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        
        
        // handle start subscription
        client.handle((i, o) -> {
            o.options(NettyPipeline.SendOptions::flushOnEach)
                    .sendString(Mono.just(startMessage))
                    .then()
                    .log("start")
                    .subscribe();

            return i.receive()
                    .asString()
                    .log("data")
                    .doOnSubscribe(action)
                    .subscribeWith(data);
        })
        .collectList()
        .subscribe();

        return data;
    }

    @Step
    public void verifyData(ReplayProcessor<String> data, String...messages) {
        
        StepVerifier.create(data)
                    .expectNext(messages)
                    .expectComplete()
                    .verify(TIMEOUT);
    }
    
}
