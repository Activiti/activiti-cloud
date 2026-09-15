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
package org.activiti.cloud.services.query.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.services.query.subscription.RegistryMessageType;
import org.activiti.cloud.services.query.subscription.SubscriberRegistryMessage;
import org.activiti.cloud.services.query.subscription.SubscriberRegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

class SubscriberResyncResponderTest {

    private static final String SOURCE_ID = "instance-a";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final List<SubscriberRegistryMessage> sent = new ArrayList<>();
    private final MessageChannel channel = (message, timeout) -> {
        sent.add((SubscriberRegistryMessage) message.getPayload());
        return true;
    };
    private SubscriberResyncResponder responder;

    @BeforeEach
    void setUp() {
        SubscriberRegistrySnapshot registry = () ->
            List.of(new SubscriberRegistryMessage.Entry("alice", List.of("dev")));
        responder = new SubscriberResyncResponder(registry, channel, SOURCE_ID, CLOCK);
    }

    @Test
    void repliesWithSnapshot_toResyncRequestFromAnotherInstance() {
        responder.accept(message(SubscriberRegistryMessage.resyncRequest("instance-b", NOW)));

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage reply = sent.getFirst();
        assertThat(reply.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(reply.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(reply.entries()).hasSize(1);
        assertThat(reply.sentAt()).isEqualTo(NOW);
    }

    @Test
    void repliesWithAnEmptySnapshot_whenNoSubscribersAreLive() {
        SubscriberResyncResponder emptyResponder = new SubscriberResyncResponder(List::of, channel, SOURCE_ID, CLOCK);

        emptyResponder.accept(message(SubscriberRegistryMessage.resyncRequest("instance-b", NOW)));

        assertThat(sent).hasSize(1);
        SubscriberRegistryMessage reply = sent.getFirst();
        assertThat(reply.type()).isEqualTo(RegistryMessageType.SNAPSHOT);
        assertThat(reply.entries()).isEmpty();
        assertThat(reply.sourceId()).isEqualTo(SOURCE_ID);
    }

    @Test
    void ignoresItsOwnResyncRequest() {
        responder.accept(message(SubscriberRegistryMessage.resyncRequest(SOURCE_ID, NOW)));

        assertThat(sent).isEmpty();
    }

    @Test
    void ignoresNonResyncMessages() {
        responder.accept(message(SubscriberRegistryMessage.registered("alice", List.of("dev"), "instance-b", NOW)));

        assertThat(sent).isEmpty();
    }

    private static Message<SubscriberRegistryMessage> message(SubscriberRegistryMessage payload) {
        return new GenericMessage<>(payload);
    }
}
