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

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

public class ErrorHandlerDefinitionMessageRecoverer implements Consumer<ErrorMessage> {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlerDefinitionMessageRecoverer.class);
    private final MessageRecoverer messageRecoverer;
    private Collection<Class<? extends Throwable>> includes = Set.of();
    private Collection<Class<? extends Throwable>> excludes = Set.of();

    public ErrorHandlerDefinitionMessageRecoverer(MessageRecoverer messageRecoverer) {
        this.messageRecoverer = messageRecoverer;
    }

    public ErrorHandlerDefinitionMessageRecoverer withRecoverableExceptions(
        Map<Class<? extends Throwable>, Boolean> recoverableExceptions
    ) {
        this.includes = recoverableExceptions
            .entrySet()
            .stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        this.excludes = recoverableExceptions
            .entrySet()
            .stream()
            .filter(Predicate.not(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        return this;
    }

    @Override
    public void accept(ErrorMessage errorMessage) {
        final var cause = NestedExceptionUtils.getMostSpecificCause(errorMessage.getPayload());
        log.warn("Attempting recovery due to cause: {}", cause.getMessage());

        Optional.of(errorMessage)
            .filter(this::isRecoverable)
            .map(ErrorMessage::getOriginalMessage)
            .ifPresentOrElse(
                originalMessage ->
                    Optional.of(originalMessage)
                        .flatMap(this::amqpMessage)
                        .ifPresentOrElse(
                            amqpMessage -> messageRecoverer.recover(amqpMessage, cause),
                            () -> {
                                log.warn(
                                    "Failed recovery attempt due to missing source amqp message {}",
                                    errorMessage.getOriginalMessage()
                                );
                            }
                        ),
                () ->
                    Optional.of(errorMessage)
                        .filter(Predicate.not(this::isIgnored))
                        .map(ErrorMessage::getPayload)
                        .ifPresentOrElse(
                            exception -> {
                                log.warn("Rethrow error message: {}", errorMessage, exception);
                                throw new MessageRejectedException(
                                    errorMessage.getOriginalMessage(),
                                    "Message recovery rejected",
                                    exception
                                );
                            },

                            () -> log.warn("Ignored recovery attempt for error message: {}", errorMessage)
                        )
            );
    }

    protected boolean match(Throwable exception) {
        Assert.notNull(exception, "Throwable to match must not be null");

        if (includes.isEmpty() && excludes.isEmpty()) {
            return true;
        }
        if (!excludes.isEmpty() && isExcluded(exception)) {
            return false;
        }
        return includes.isEmpty() || isIncluded(exception);
    }

    protected boolean isExcluded(Throwable cause) {
        return excludes.stream().anyMatch(containsNestedException(cause));
    }

    protected boolean isIncluded(Throwable cause) {
        return includes.stream().anyMatch(containsNestedException(cause));
    }

    protected <T extends Throwable> Optional<T> findCause(Throwable throwable, Class<T> targetType) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        while (throwable != null && seen.add(throwable)) {
            if (targetType.isInstance(throwable)) {
                return (Optional<T>) Optional.of(throwable);
            }
            throwable = throwable.getCause();
        }
        return Optional.empty();
    }

    protected Predicate<Class<? extends Throwable>> containsNestedException(Throwable throwable) {
        return exType -> findCause(throwable, exType).isPresent();
    }

    protected boolean isRecoverable(ErrorMessage errorMessage) {
        return Optional.of(errorMessage.getPayload()).filter(this::match).isPresent();
    }

    protected boolean isIgnored(ErrorMessage errorMessage) {
        return Optional.of(errorMessage.getPayload()).filter(this::isExcluded).isPresent();
    }

    protected Optional<Message> amqpMessage(org.springframework.messaging.Message<?> originalMessage) {
        return Optional.of(originalMessage)
            .map(StaticMessageHeaderAccessor::getSourceData)
            .filter(Message.class::isInstance)
            .map(Message.class::cast);
    }
}
