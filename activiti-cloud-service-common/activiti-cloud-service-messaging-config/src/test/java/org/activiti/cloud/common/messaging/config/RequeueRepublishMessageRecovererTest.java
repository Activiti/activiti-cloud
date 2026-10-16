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
package org.activiti.cloud.common.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.core.NestedExceptionUtils;

@ExtendWith(MockitoExtension.class)
class RequeueRepublishMessageRecovererTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MessageRecoverer republishMessageRecoverer;

    private RequeueRepublishMessageRecoverer recoverer;
    private Message message;
    private MessageProperties properties;
    private Throwable exception;

    @BeforeEach
    void setUp() {
        recoverer = new RequeueRepublishMessageRecoverer(rabbitTemplate, republishMessageRecoverer);
        properties = new MessageProperties();
        message = new Message(new byte[0], properties);
        exception = new RuntimeException("Original error", new IllegalArgumentException("Specific root cause"));
    }

    @Test
    @DisplayName("Should increment retry count and requeue message when current retry is below maxRetries")
    void shouldRequeueMessageWhenUnderMaxRetries() {
        // Arrange
        properties.setReceivedExchange("my-exchange");
        properties.setReceivedRoutingKey("my-routing-key");
        // Implicitly starting at 0 retries

        // Act
        recoverer.recover(message, exception);

        // Assert
        assertThat(properties.getRetryCount()).isEqualTo(1);
        assertThat(properties.isRedelivered()).isTrue();

        verify(rabbitTemplate).send("my-exchange", "my-routing-key", message);
        verifyNoInteractions(republishMessageRecoverer);
    }

    @Test
    @DisplayName("Should use custom DLX exchange and consumer queue as routing key when dlxName is configured")
    void shouldRequeueWithDlxCoordinatesWhenDlxNameIsSet() {
        // Arrange
        recoverer.dlxName("my-dlx-exchange");
        properties.setConsumerQueue("my-queue");
        properties.setReceivedExchange("ignored-exchange");
        properties.setReceivedRoutingKey("ignored-routing-key");

        // Act
        recoverer.recover(message, exception);

        // Assert
        assertThat(properties.getRetryCount()).isEqualTo(1);
        verify(rabbitTemplate).send("my-dlx-exchange", "my-queue", message);
        verifyNoInteractions(republishMessageRecoverer);
    }

    @Test
    @DisplayName("Should route to republishMessageRecoverer with specific cause when maxRetries is reached")
    void shouldRepublishToParkingLotWhenMaxRetriesExceeded() {
        // Arrange
        recoverer.maxRetries(3);
        // Force the message to already be at the threshold limit
        properties.setRetryCount(3);

        // Act
        recoverer.recover(message, exception);

        // Assert
        // The retry count shouldn't change further on final failure fallback
        assertThat(properties.getRetryCount()).isEqualTo(3);

        // Assert that the most specific cause was passed down
        Throwable expectedSpecificCause = NestedExceptionUtils.getMostSpecificCause(exception);
        assertThat(expectedSpecificCause).isInstanceOf(IllegalArgumentException.class);

        verify(republishMessageRecoverer).recover(message, expectedSpecificCause);
        verifyNoInteractions(rabbitTemplate);
    }
}
