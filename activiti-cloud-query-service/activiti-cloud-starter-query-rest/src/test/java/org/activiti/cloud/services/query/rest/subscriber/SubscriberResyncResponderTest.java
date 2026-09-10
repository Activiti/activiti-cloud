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
package org.activiti.cloud.services.query.rest.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.support.GenericMessage;

class SubscriberResyncResponderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String DESTINATION = "subscriberRegistry";
    private static final String SOURCE_ID = "rest-1";

    private SubscriberRegistry registry;
    private StreamBridge streamBridge;
    private SubscriberResyncResponder responder;

    @BeforeEach
    void setUp() {
        registry = new SubscriberRegistry(mock(ApplicationEventPublisher.class), 50_000);
        registry.register("alice", Set.of("eng"), "session-1", NOW);
        streamBridge = mock(StreamBridge.class);
        responder = new SubscriberResyncResponder(
            registry,
            streamBridge,
            SOURCE_ID,
            DESTINATION,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void should_replyWithASnapshotOfItsRegistry_when_anotherInstanceRequestsResync() {
        responder.accept(new GenericMessage<>(SubscriberRegistryMessage.resyncRequest("consumer-1", NOW)));

        ArgumentCaptor<SubscriberRegistryMessage> captor = ArgumentCaptor.forClass(SubscriberRegistryMessage.class);
        verify(streamBridge).send(eq(DESTINATION), captor.capture());
        SubscriberRegistryMessage snapshot = captor.getValue();
        assertThat(snapshot.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(snapshot.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(snapshot.sentAt()).isEqualTo(NOW);
        assertThat(snapshot.entries()).extracting(SubscriberRegistryMessage.Entry::userId).containsExactly("alice");
    }

    @Test
    void should_ignoreItsOwnEcho_when_theResyncRequestCarriesItsOwnSourceId() {
        responder.accept(new GenericMessage<>(SubscriberRegistryMessage.resyncRequest(SOURCE_ID, NOW)));

        verify(streamBridge, never()).send(any(), any());
    }

    @Test
    void should_ignoreTheMessage_when_itIsNotAResyncRequest() {
        responder.accept(
            new GenericMessage<>(SubscriberRegistryMessage.registered("bob", List.of("eng"), "consumer-1", NOW))
        );

        verify(streamBridge, never()).send(any(), any());
    }
}
