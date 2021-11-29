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
package org.activiti.cloud.services.core.conf;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.model.shared.Payload;
import org.activiti.cloud.services.core.commands.CommandEndpointAdminAuthentication;
import org.activiti.cloud.services.core.commands.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@ConditionalOnProperty(name = "activiti.stream.cloud.functional.binding", havingValue = "enabled")
public class CommandBindingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CommandBindingConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "commandProcessor")
    public <T extends Payload, R> Function<T, R> commandProcessor(Set<CommandExecutor<T>> cmdExecutors) {
        return (payload) -> {
            SecurityContextHolder.getContext()
                    .setAuthentication(new CommandEndpointAdminAuthentication());
            try {

                Map<String, CommandExecutor<T>> commandExecutors = cmdExecutors.stream()
                        .collect(Collectors.toMap(CommandExecutor::getHandledType,
                                Function.identity()));

                return (R)processCommand(payload, commandExecutors);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private <T extends Payload> Object processCommand(T payload, Map<String, CommandExecutor<T>> commandExecutors) {
        CommandExecutor<T> cmdExecutor = commandExecutors.get(payload.getClass().getName());
        if (cmdExecutor != null) {
            return cmdExecutor.execute(payload);

        } else {
            logger.warn(">>> No Command Found for type: " + payload.getClass());
        }
        return new EmptyResult();
    }
}
