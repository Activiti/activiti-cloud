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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class CommandConsumerBinding {

    private static final Logger logger = LoggerFactory.getLogger(CommandConsumerBinding.class);

    @Bean
    @ConditionalOnMissingBean
    public <T extends Payload, R> Function<T, R> commandConsumer(Set<CommandExecutor<T>> cmdExecutors) {
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
