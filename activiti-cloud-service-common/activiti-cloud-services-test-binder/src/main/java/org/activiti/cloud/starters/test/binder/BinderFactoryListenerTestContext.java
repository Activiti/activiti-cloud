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
package org.activiti.cloud.starters.test.binder;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DeclarableCustomizer;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class BinderFactoryListenerTestContext {

    public Map<String, Queue> getQueues() {
        return queues;
    }

    public Map<String, Queue> getAnonymousQueues() {
        return anonymousQueues;
    }

    public Map<String, Exchange> getExchanges() {
        return exchanges;
    }

    public Map<String, Binding> getBindings() {
        return bindings;
    }

    private final Map<String, Queue> queues = new LinkedHashMap<>();
    private final Map<String, Queue> anonymousQueues = new LinkedHashMap<>();
    private final Map<String, Exchange> exchanges = new LinkedHashMap<>();
    private final Map<String, Binding> bindings = new LinkedHashMap<>();

    public void afterTestClass() {
        queues.clear();
        exchanges.clear();
        anonymousQueues.clear();
        bindings.clear();
    }

    @Bean
    DeclarableCustomizer binderFactoryListenerDeclarableCustomizer() {
        return declarable -> {
            if (declarable instanceof AnonymousQueue anonymousQueue) {
                anonymousQueues.computeIfAbsent(anonymousQueue.getName(), key -> anonymousQueue);
            } else if (declarable instanceof Queue queue) {
                queues.computeIfAbsent(queue.getName(), key -> queue);
            } else if (declarable instanceof Exchange exchange) {
                exchanges.computeIfAbsent(exchange.getName(), key -> exchange);
            } else if (declarable instanceof Binding binding) {
                bindings.computeIfAbsent(binding.toString(), key -> binding);
            }

            return declarable;
        };
    }
}
