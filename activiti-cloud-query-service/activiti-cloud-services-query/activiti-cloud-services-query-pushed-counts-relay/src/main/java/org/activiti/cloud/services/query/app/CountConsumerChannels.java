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

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

/**
 * The channel {@code @FunctionBinding(input = COUNT_CONSUMER)} attaches to. Without this, Spring Cloud
 * Stream never learns that {@code countConsumer} is a real binding to declare on the broker - the
 * function bean would still exist, but only wired to an unbound, in-memory channel.
 */
public interface CountConsumerChannels {
    String COUNT_CONSUMER = "countConsumer";

    @InputBinding(COUNT_CONSUMER)
    default SubscribableChannel countConsumer() {
        return MessageChannels.publishSubscribe(COUNT_CONSUMER).getObject();
    }
}
