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

package org.activiti.cloud.services.core.commands;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.model.shared.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommandEndpoint<T extends Payload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEndpoint.class);
    private Map<String, CommandExecutor<T>> commandExecutors;

    public CommandEndpoint(Set<CommandExecutor<T>> cmdExecutors) {
        this.commandExecutors = cmdExecutors.stream()
                                            .collect(Collectors.toMap(CommandExecutor::getHandledType,
                                                                      Function.identity()));
    }

    public <R> R execute(T payload) {

        SecurityContextHolder.getContext()
                             .setAuthentication(new CommandEndpointAdminAuthentication());
        try {
            return (R) processCommand(payload);

        } finally {
            SecurityContextHolder.clearContext();
        }

    }

    private Object processCommand(T payload) {

        CommandExecutor<T> cmdExecutor = commandExecutors.get(payload.getClass().getName());
        if (cmdExecutor != null) {
            return cmdExecutor.execute(payload);

        } else {
            LOGGER.warn(">>> No Command Found for type: " + payload.getClass());
        }

        return new EmptyResult();
    }
}
