/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.listeners;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandContextEventsAggregator {

    private final MessageProducerCommandContextCloseListener closeListener;

    @Autowired
    public CommandContextEventsAggregator(MessageProducerCommandContextCloseListener closeListener) {
        this.closeListener = closeListener;
    }

    public void add(ProcessEngineEvent event) {

        CommandContext currentCommandContext = getCurrentCommandContext();
        List<ProcessEngineEvent> events = currentCommandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS);
        if (events == null) {
            events = new ArrayList<>();
            currentCommandContext.addAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS,
                                               events);
        }
        events.add(event);

        if (!currentCommandContext.hasCloseListener(MessageProducerCommandContextCloseListener.class)) {
            currentCommandContext.addCloseListener(closeListener);
        }

    }

    protected CommandContext getCurrentCommandContext() {
        return Context.getCommandContext();
    }

}
