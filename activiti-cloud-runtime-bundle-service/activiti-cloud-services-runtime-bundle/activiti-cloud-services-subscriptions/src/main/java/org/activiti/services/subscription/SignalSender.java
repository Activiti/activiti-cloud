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
package org.activiti.services.subscription;

import java.util.Objects;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.runtime.api.signal.SignalPayloadEventListener;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Overrides default SignalPayloadEventListener implementation to
 * broadcast signals into Runtime Bundle instances via Cloud Stream
 */
public class SignalSender implements SignalPayloadEventListener {

    private final MessageChannel messageChannel;
    private final StreamBridge streamBridge;

    public SignalSender(MessageChannel messageChannel) {
        this.messageChannel = messageChannel;
        this.streamBridge = null;
    }

    public SignalSender(StreamBridge streamBridge) {
        this.messageChannel = null;
        this.streamBridge = streamBridge;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendSignal(SignalPayload signalPayload) {
        Message<SignalPayload> message = MessageBuilder.withPayload(signalPayload).build();
        if (Objects.nonNull(messageChannel)) {
            messageChannel.send(message);
        } else {
            streamBridge.send("", "a");
        }
    }
}
