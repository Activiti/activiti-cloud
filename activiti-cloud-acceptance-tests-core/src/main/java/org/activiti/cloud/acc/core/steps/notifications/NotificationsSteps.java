package org.activiti.cloud.acc.core.steps.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.activiti.cloud.acc.core.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessRuntimeService;
import org.springframework.beans.factory.annotation.Autowired;

import net.thucydides.core.annotations.Step;
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
    private static final Duration TIMEOUT = Duration.ofMillis(20000);
    
    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;
    
    @Autowired
    private RuntimeTestsConfigurationProperties properties;

    @Autowired
    private ProcessRuntimeService processRuntimeService;

    @Step
    public void checkServicesHealth() {
        assertThat(processRuntimeService.isServiceUp()).isTrue();
    }
    
    @Step
    public ReplayProcessor<String> subscribe(String accessToken) throws InterruptedException {
        ReplayProcessor<String> data = ReplayProcessor.create();
        
        WebsocketSender client = HttpClient.create()
                .wiretap(true)
                .headers(h -> h.add(AUTHORIZATION, "Bearer " + accessToken))
                .websocket(GRAPHQL_WS)
                .uri(properties.getGraphqlWsUrl());
        
        String startMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"query\":\""
        		+ "subscription {" + 
        		"  engineEvents(serviceName: \\\"rb-my-app\\\", processDefinitionKey: \\\"ConnectorProcess\\\" ) {" + 
        		"    appName" + 
        		"    serviceName" + 
        		"    processDefinitionKey" + 
        		"    PROCESS_STARTED {" + 
        		"      entity {" + 
        		"        status" + 
        		"      }" + 
        		"    }" + 
        		"    PROCESS_COMPLETED {" + 
        		"      entity {" + 
        		"        status" + 
        		"      }" + 
        		"    }" + 
        		"  }" + 
        		"}"
        		+ "\",\"variables\":null}}";
        
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
                        .take(2) // stop subscription after 2 data messages received
                        .subscribeWith(data);
                }) 
                .collectList()
                .subscribe();

        // Let's wait for subscribe 
        Thread.sleep(1000);
        
        return data;
    }
    
    @Step
    public void verifyData(ReplayProcessor<String> data) {
        
        String startProcessMessage = "{\"payload\":{\"data\":{\"engineEvents\":{\"appName\":\"default-app\",\"serviceName\":\"rb-my-app\",\"processDefinitionKey\":\"ConnectorProcess\",\"PROCESS_STARTED\":[{\"entity\":{\"status\":\"RUNNING\"}}],\"PROCESS_COMPLETED\":null}}},\"id\":\"1\",\"type\":\"data\"}";
        String completeProcessMessage = "{\"payload\":{\"data\":{\"engineEvents\":{\"appName\":\"default-app\",\"serviceName\":\"rb-my-app\",\"processDefinitionKey\":\"ConnectorProcess\",\"PROCESS_STARTED\":null,\"PROCESS_COMPLETED\":[{\"entity\":{\"status\":\"COMPLETED\"}}]}}},\"id\":\"1\",\"type\":\"data\"}";

		StepVerifier.create(data)
		            .expectNext(startProcessMessage)
		            .expectNext(completeProcessMessage)
		            .expectComplete()
		            .verify(TIMEOUT);
    }
    
}
