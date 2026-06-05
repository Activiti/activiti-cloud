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

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class QueryEventsProducer {

    public static final String QUERY_EVENTS_TASK_EXECUTOR = "queryEventsTaskExecutor";

    private final MessageChannel queryEventsProducer;

    public QueryEventsProducer(MessageChannel queryEventsProducer) {
        this.queryEventsProducer = queryEventsProducer;
    }

    @Async(QUERY_EVENTS_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueryEngineEventsHandled(QueryEngineEventsHandledEvent event) {
        queryEventsProducer.send(MessageBuilder.withPayload(event.events()).build());
    }
}