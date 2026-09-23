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
package org.activiti.cloud.services.messages.core.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

class MessageConnectorAggregatorTest {

    private final MessageGroupProcessor processor = group -> null;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void should_preserveInterruptAndSkipCompletion_when_cleanupIsInterrupted() {
        MessageGroupStore messageStore = mock(MessageGroupStore.class);
        TestableMessageConnectorAggregator aggregator = new TestableMessageConnectorAggregator(processor, messageStore);
        aggregator.setCompleteGroupsWhenEmpty(true);

        Message<String> message = MessageBuilder.withPayload("message").build();
        MessageGroup messageGroup = mock(MessageGroup.class);
        when(messageGroup.getGroupId()).thenReturn("groupId");
        when(messageGroup.getMessages()).thenReturn(List.<Message<?>>of(message));
        when(messageStore.messageGroupSize("groupId")).thenThrow(
            new RuntimeException(new InterruptedException("boom"))
        );

        assertThatCode(() ->
            aggregator.invokeAfterRelease(messageGroup, List.<Message<?>>of(message))
        ).doesNotThrowAnyException();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(messageStore).removeMessagesFromGroup("groupId", List.<Message<?>>of(message));
    }

    @Test
    void should_rethrowNonInterruptedCleanupFailure() {
        MessageGroupStore messageStore = mock(MessageGroupStore.class);
        TestableMessageConnectorAggregator aggregator = new TestableMessageConnectorAggregator(processor, messageStore);
        aggregator.setCompleteGroupsWhenEmpty(true);

        Message<String> message = MessageBuilder.withPayload("message").build();
        MessageGroup messageGroup = mock(MessageGroup.class);
        when(messageGroup.getGroupId()).thenReturn("groupId");
        when(messageGroup.getMessages()).thenReturn(List.<Message<?>>of(message));
        when(messageStore.messageGroupSize("groupId")).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aggregator.invokeAfterRelease(messageGroup, List.<Message<?>>of(message)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }

    private static final class TestableMessageConnectorAggregator extends MessageConnectorAggregator {

        private TestableMessageConnectorAggregator(MessageGroupProcessor processor, MessageGroupStore store) {
            super(processor, store);
        }

        private void invokeAfterRelease(MessageGroup messageGroup, List<Message<?>> completedMessages) {
            afterRelease(messageGroup, completedMessages);
        }
    }
}
