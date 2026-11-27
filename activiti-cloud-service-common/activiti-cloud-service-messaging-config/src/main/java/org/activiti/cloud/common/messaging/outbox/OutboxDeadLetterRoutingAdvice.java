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

package org.activiti.cloud.common.messaging.outbox;

import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.ExceptionTypeFilter;

public class OutboxDeadLetterRoutingAdvice extends AbstractRequestHandlerAdvice implements Ordered {

    private final ExceptionTypeFilter outboxExceptionsTypeFilter;
    private final MessageChannel deadLetterChannel;

    public OutboxDeadLetterRoutingAdvice(
        ExceptionTypeFilter outboxExceptionsTypeFilter,
        MessageChannel deadLetterChannel
    ) {
        this.outboxExceptionsTypeFilter = outboxExceptionsTypeFilter;
        this.deadLetterChannel = deadLetterChannel;
    }

    @Override
    protected Object doInvoke(
        @NonNull ExecutionCallback callback,
        @NonNull Object target,
        @NonNull Message<?> message
    ) {
        try {
            return callback.execute();
        } catch (Exception exception) {
            Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);

            if (isRetryable(exception)) {
                // Rethrow to trigger transactional poller rollback (row remains in INT_CHANNEL_MESSAGE)
                throw exception;
            }

            // Poison pill (e.g., SerializationException, 400 Bad Request, ClassCastException):
            // Forward to DLQ with error metadata, then swallow exception so poller TX commits & deletes row.
            Message<?> dlqMessage = MessageBuilder.fromMessage(message)
                .setHeader(
                    "x-error-message",
                    Optional.ofNullable(rootCause.getMessage())
                        .or(() -> Optional.ofNullable(exception.getCause()).map(Throwable::getMessage))
                        .orElse(null)
                )
                .setHeader("x-error-class", rootCause.getClass().getName())
                .build();

            deadLetterChannel.send(dlqMessage);
            return null;
        }
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        return outboxExceptionsTypeFilter.match(throwable, true);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
