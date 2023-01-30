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
package org.activiti.cloud.connectors.starter.config;

import java.util.function.Consumer;
import java.util.logging.Level;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.activiti.cloud.connectors.starter.test.it.ConnectorsITStreamHandlers;
import org.activiti.cloud.connectors.starter.test.it.RuntimeMockStreams;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

@Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
@Configuration
public class ConnectorsITStreamHandlersConfiguration {

    @FunctionBinding(input = RuntimeMockStreams.INTEGRATION_RESULT_CONSUMER)
    @Bean
    public Consumer<Flux<IntegrationResult>> integrationMockResultConsumer(ConnectorsITStreamHandlers streamHandlers) {
        return flux -> flux.log("integrationMockResultConsumer", Level.INFO).subscribe(result -> streamHandlers.consumeIntegrationResultsMock(result));
    }

    @FunctionBinding(input = RuntimeMockStreams.INTEGRATION_ERROR_CONSUMER)
    @Bean
    public Consumer<Message<IntegrationError>> integrationMockErrorConsumer(ConnectorsITStreamHandlers streamHandlers) {
        return result -> streamHandlers.consumeIntegrationErrorMock(result.getPayload());
    }
}
