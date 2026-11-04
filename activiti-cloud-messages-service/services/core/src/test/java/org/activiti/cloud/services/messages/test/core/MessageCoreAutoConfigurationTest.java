/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.messages.test.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.cloud.services.messages.core.channels.MessageConnectorProcessor;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(properties = "activiti.cloud.application.name=my-activiti-rb-app")
@Import(TestChannelBinderConfiguration.class)
public class MessageCoreAutoConfigurationTest {

    @Autowired
    private MessageConnectorProcessor messageConnectorProcessor;

    @Autowired
    private IntegrationFlow messageConnectorIntegrationFlow;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PublishSubscribeChannel discardChannel;

    @SpringBootApplication
    static class Application {}

    @Test
    public void contextLoads() {
        assertThat(messageConnectorProcessor).isNotNull();
        assertThat(messageConnectorIntegrationFlow).isNotNull();

        assertProperty("spring.cloud.stream.bindings.messageConnectorInput.destination").isEqualTo("messageEvents");
        assertProperty("spring.cloud.stream.bindings.messageConnectorOutput.destination").isEqualTo("commandConsumer");
    }

    @Test
    void discardChannelSubscriberCount() {
        assertThat(discardChannel.getSubscriberCount()).isEqualTo(1);
    }

    @Test
    void shouldDeserializeMessageEventPayloadWithTypeHeader() {
        var transformer = applicationContext.getBean(JsonToObjectTransformer.class);
        var message = MessageBuilder.withPayload("{}")
            .setHeader(JsonHeaders.TYPE_ID, MessageEventPayload.class.getName())
            .build();

        assertThat(transformer.transform(message).getPayload()).isInstanceOf(MessageEventPayload.class);
    }

    @Test
    void shouldRejectUntrustedPayloadTypeHeader() {
        var transformer = applicationContext.getBean(JsonToObjectTransformer.class);
        var message = MessageBuilder.withPayload("{}").setHeader(JsonHeaders.TYPE_ID, "java.io.File").build();

        assertThatThrownBy(() -> transformer.transform(message))
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasStackTraceContaining("java.io.File")
            .hasStackTraceContaining("not in the trusted packages");
    }

    private AbstractStringAssert<?> assertProperty(String name) {
        return assertThat(getProperty(name));
    }

    private String getProperty(String name) {
        return applicationContext.getEnvironment().getProperty(name);
    }
}
