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
package org.activiti.cloud.messages.integration.tests.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "activiti.cloud.messaging.function-router.enabled=true" })
class MessageEventsFunctionRouterIT extends MessageEventsIT {

    @Autowired(required = false)
    private Consumer<Message<?>> functionRouterConsumer;

    @Autowired(required = false)
    private MessageChannel functionRouterInput;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Test
    void functionRouterConsumer() {
        assertThat(functionRouterConsumer).isNotNull();
    }

    @Test
    void functionRouterInput() {
        assertThat(functionRouterInput).isNotNull();
    }

    @Test
    void bindingServiceProperties() {
        assertThat(bindingServiceProperties.getBindings()).containsKey("functionRouterInput");
    }

    @Test
    void messagingProperties() {
        assertThat(messagingProperties.getFunctionRouter().isFunctionRoute("messageConnectorInput")).isTrue();
    }
}
