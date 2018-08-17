package org.activiti.cloud.services.core.commands;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.api.model.shared.Payload;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;

@Component
public class CommandEndpoint<T extends Payload> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEndpoint.class);
    private Map<String, CommandExecutor<T>> commandExecutors;

    @Autowired
    public CommandEndpoint(Set<CommandExecutor<T>> cmdExecutors) {
        this.commandExecutors = cmdExecutors.stream().collect(Collectors.toMap(CommandExecutor::getHandledType,
                                                                               Function.identity()));
    }

    @StreamListener(ProcessEngineChannels.COMMAND_CONSUMER)
    public void consumeActivateProcessInstanceCmd(T payload) {
        // Security set to THREADLOCAL and Admin user set for Command Executors
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
        SecurityContextHolder.setContext(new SecurityContextImpl(new CommandEndpointAdminAuthentication()));

        processCommand(payload);
    }

    private void processCommand(T payload) {

        CommandExecutor<T> cmdExecutor = commandExecutors.get(payload.getClass().getName());
        if (cmdExecutor != null) {
            cmdExecutor.execute(payload);
        } else {
            LOGGER.warn(">>> No Command Found for type: " + payload.getClass());
        }
    }
}
