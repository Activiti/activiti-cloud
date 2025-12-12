/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.events.services;

import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.MessageBuilderChainFactory;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.springframework.messaging.Message;

class CreateIncidentEventFromExecutionCmd implements CreateIncidentEventCmd {

    private final ExecutionContext executionContext;
    private final Exception exception;
    private final MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory;
    private final RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    CreateIncidentEventFromExecutionCmd(
        ExecutionContext executionContext,
        Exception exception,
        MessageBuilderChainFactory<ExecutionContext> messageBuilderIncidentsChainFactory,
        RuntimeBundleInfoAppender runtimeBundleInfoAppender
    ) {
        this.executionContext = executionContext;
        this.exception = exception;
        this.messageBuilderIncidentsChainFactory = messageBuilderIncidentsChainFactory;
        this.runtimeBundleInfoAppender = runtimeBundleInfoAppender;
    }

    @Override
    public Message execute(CommandContext commandContext) {
        return createMessage(this.executionContext, this.exception);
    }

    @Override
    public MessageBuilderChainFactory<ExecutionContext> getMessageBuilderIncidentsChainFactory() {
        return this.messageBuilderIncidentsChainFactory;
    }

    @Override
    public RuntimeBundleInfoAppender getRuntimeBundleInfoAppender() {
        return this.runtimeBundleInfoAppender;
    }
}
