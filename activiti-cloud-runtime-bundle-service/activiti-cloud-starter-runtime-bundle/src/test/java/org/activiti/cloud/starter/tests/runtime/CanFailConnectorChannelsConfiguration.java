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
package org.activiti.cloud.starter.tests.runtime;

import java.util.function.Consumer;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.activiti.cloud.common.messaging.functional.FunctionBinding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;

@TestConfiguration
@Import(CanFailConnector.class)
public class CanFailConnectorChannelsConfiguration implements CanFailConnectorChannels {

    @Bean(CAN_FAIL_CONNECTOR)
    @ConditionalOnMissingBean(name = CAN_FAIL_CONNECTOR)
    @Override
    public SubscribableChannel canFailConnector() {
        return MessageChannels.publishSubscribe(CAN_FAIL_CONNECTOR)
            .get();
    }

    @FunctionBinding(input = CAN_FAIL_CONNECTOR)
    @Bean("canFailConnectorConsumer")
    public Consumer<Message<IntegrationRequest>> canFailConnector(CanFailConnector canFailConnector) {
        return message -> {
            canFailConnector.canFailConnector(message);
        };
    }
}
