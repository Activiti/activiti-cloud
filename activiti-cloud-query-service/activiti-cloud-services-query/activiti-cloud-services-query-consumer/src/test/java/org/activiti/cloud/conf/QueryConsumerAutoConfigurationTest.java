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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.query.app.QueryConsumerMessageHandler;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.activiti.cloud.services.query.events.handlers.cleanup.variable.RuntimeVariableCleanup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_createHandlerWithOptionalPayloadCleanup(boolean cleanupEnabled) {
        var beans = new DefaultListableBeanFactory();
        var payloadCleanup = mock(RuntimeVariableCleanup.class);
        if (cleanupEnabled) {
            beans.registerSingleton("payloadCleanup", payloadCleanup);
        }
        var optimizer = mock(QueryEventHandlerContextOptimizer.class);
        var flow = mock(IntegrationFlow.class);
        var channel = mock(MessageChannel.class);
        given(flow.getInputChannel()).willReturn(channel);
        QueryConsumerMessageHandler handler = new QueryConsumerAutoConfiguration().queryConsumerMessageHandler(
            mock(QueryEventHandlerContext.class),
            optimizer,
            mock(EntityManager.class),
            flow,
            100,
            beans.getBeanProvider(RuntimeVariableCleanup.class)
        );
        List<CloudRuntimeEvent<?, ?>> events = List.of(new CloudProcessCreatedEventImpl());
        var message = MessageBuilder.withPayload(events).build();
        given(optimizer.optimize(events)).willReturn(events);

        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx -> handler.accept(message));

        if (cleanupEnabled) {
            verify(payloadCleanup).cleanup(events);
        } else {
            verifyNoInteractions(payloadCleanup);
        }
        verify(channel).send(message);
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
