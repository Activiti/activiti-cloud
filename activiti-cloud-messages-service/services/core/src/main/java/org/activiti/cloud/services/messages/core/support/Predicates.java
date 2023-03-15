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
package org.activiti.cloud.services.messages.core.support;

import java.util.function.Predicate;
import org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents;
import org.activiti.api.process.model.events.MessageDefinitionEvent.MessageDefinitionEvents;
import org.activiti.api.process.model.events.MessageSubscriptionEvent.MessageSubscriptionEvents;
import org.activiti.cloud.services.messages.core.support.MessageSelectors.MessageEventTypeSelector;
import org.springframework.messaging.Message;

public class Predicates {

    public static Predicate<Message<?>> MESSAGE_WAITING = predicate(
        new MessageEventTypeSelector(MessageEvents.MESSAGE_WAITING)::accept
    );
    public static Predicate<Message<?>> MESSAGE_RECEIVED = predicate(
        new MessageEventTypeSelector(MessageEvents.MESSAGE_RECEIVED)::accept
    );
    public static Predicate<Message<?>> MESSAGE_SENT = predicate(
        new MessageEventTypeSelector(MessageEvents.MESSAGE_SENT)::accept
    );
    public static Predicate<Message<?>> MESSAGE_SUBSCRIPTION_CANCELLED = predicate(
        new MessageEventTypeSelector(MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED)::accept
    );
    public static Predicate<Message<?>> START_MESSAGE_DEPLOYED = predicate(
        new MessageEventTypeSelector(MessageDefinitionEvents.START_MESSAGE_DEPLOYED)::accept
    );

    public static <T> Predicate<T> predicate(Predicate<T> predicate) {
        return predicate;
    }

    public static <R> Predicate<R> not(Predicate<R> predicate) {
        return predicate.negate();
    }
}
