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
package org.activiti.cloud.conf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringIntegrationTest
@SpringJUnitConfig(QueryConsumerAutoConfigurationTest.TestConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class QueryConsumerAutoConfigurationTest {

    @Autowired
    private MessageChannel partitionedQueryConsumerErrorChannel;

    @Test
    void shouldHandleErrorMessageInPartitionedQueryConsumerErrorIntegrationFlow(CapturedOutput output) {
        ErrorMessage errorMessage = new ErrorMessage(
            new MessagingException("Error message"),
            MessageBuilder.withPayload("payload").build()
        );

        assertThat(partitionedQueryConsumerErrorChannel).isNotNull();
        assertThatCode(() -> partitionedQueryConsumerErrorChannel.send(errorMessage)).doesNotThrowAnyException();
        assertThat(output).contains("Error message while handling GenericMessage [payload=payload");
    }

    @Test
    void shouldHandleUnexpectedMessageTypeInPartitionedQueryConsumerErrorIntegrationFlow(CapturedOutput output) {
        assertThatCode(() ->
            partitionedQueryConsumerErrorChannel.send(MessageBuilder.withPayload("unexpected").build())
        ).doesNotThrowAnyException();
        assertThat(output).contains(
            " Unexpected message type class org.springframework.messaging.support.GenericMessage"
        );
    }

    @Configuration
    @EnableIntegration
    static class TestConfiguration {

        @Bean
        IntegrationFlow partitionedQueryConsumerErrorIntegrationFlow() {
            return new QueryConsumerAutoConfiguration().partitionedQueryConsumerErrorIntegrationFlow();
        }
    }
}
