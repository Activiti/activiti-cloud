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

package org.activiti.cloud.services.gateway;

import static org.activiti.cloud.services.gateway.channels.ProcessRuntimeGatewayChannels.PROCESS_RUNTIME_GATEWAY_PRODUCER;
import static org.activiti.cloud.services.gateway.channels.ProcessRuntimeGatewayChannels.PROCESS_RUNTIME_GATEWAY_RESULTS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsPayload;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsResult;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.activiti.cloud.services.gateway.channels.ProcessRuntimeGatewayChannels;
import org.activiti.cloud.services.gateway.config.ProcessRuntimeGatewayProperties;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

@SpringBootTest(
    properties = {
        "spring.cloud.stream.bindings.commandConsumer.destination=commandConsumer",
        "spring.cloud.stream.bindings.commandConsumer.contentType=application/json",
        "spring.cloud.stream.bindings.commandConsumer.group=${spring.application.name}",
        "spring.cloud.stream.bindings.commandResults.destination=commandResults",
        "spring.cloud.stream.bindings.commandResults.contentType=application/json",
    }
)
@EnableTestBinder
public class ProcessRuntimeGatewayTest {

    @SpringBootApplication
    @EnableTestBinder
    static class TestApplication {

        final String COMMAND_CONSUMER = "commandConsumer";
        final String COMMAND_RESULTS = "commandResults";

        @InputBinding(COMMAND_CONSUMER)
        MessageChannel commandConsumer() {
            return MessageChannels.publishSubscribe(COMMAND_CONSUMER).getObject();
        }

        @OutputBinding(COMMAND_RESULTS)
        SubscribableChannel commandResults() {
            return MessageChannels.direct(COMMAND_RESULTS).getObject();
        }

        @FunctionBinding(input = COMMAND_CONSUMER, output = COMMAND_RESULTS)
        @Bean
        Function<SyncCloudProcessDefinitionsPayload, SyncCloudProcessDefinitionsResult> testCommandExecutor() {
            return payload -> {
                var result = Stream
                    .of("foo:1", "foo:2", "bar:1", "bar:2", "baz")
                    .filter(it -> payload.getProcessDefinitionKeys().stream().anyMatch(it::startsWith))
                    .filter(Predicate.not(payload.getExcludedProcessDefinitionIds()::contains))
                    .toList();

                return new SyncCloudProcessDefinitionsResult(payload, result);
            };
        }
    }

    @Autowired
    private ProcessRuntimeGateway processRuntimeGateway;

    @Autowired
    private ProcessRuntimeGatewayChannels processRuntimeGatewayChannels;

    @Autowired
    private ProcessRuntimeGatewayProperties processRuntimeGatewayProperties;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Test
    void contextLoads() {
        assertThat(processRuntimeGatewayProperties.getReplyTimeout()).isEqualTo(Duration.ofSeconds(30));

        assertThat(processRuntimeGatewayProperties.getGroup()).isEqualTo("query");
        assertThat(bindingServiceProperties.getGroup(PROCESS_RUNTIME_GATEWAY_RESULTS)).isEqualTo("query");

        assertThat(bindingServiceProperties.getBindingDestination(PROCESS_RUNTIME_GATEWAY_PRODUCER))
            .isEqualTo("commandConsumer_my-app");
        assertThat(bindingServiceProperties.getBindingDestination(PROCESS_RUNTIME_GATEWAY_RESULTS))
            .isEqualTo("commandResults_my-app");
    }

    @Test
    void processRuntimeGatewayTest() {
        // given
        var processDefinitionKeys = List.of("foo", "bar", "baz");
        var excludedProcessDefinitionIds = List.of("foo:1", "bar:1");

        // when
        var result = processRuntimeGateway.syncProcessDefinitions(
            SyncCloudProcessDefinitionsPayload
                .builder()
                .processDefinitionKeys(processDefinitionKeys)
                .excludedProcessDefinitionIds(excludedProcessDefinitionIds)
                .build()
        );

        // then
        assertThat(result)
            .isNotNull()
            .extracting(SyncCloudProcessDefinitionsResult::getEntity)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsOnly("foo:2", "bar:2", "baz");
    }
}
