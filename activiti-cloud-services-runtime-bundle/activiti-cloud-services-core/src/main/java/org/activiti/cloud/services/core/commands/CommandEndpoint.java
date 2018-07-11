package org.activiti.cloud.services.core.commands;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.runtime.api.cmd.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
public class CommandEndpoint<T extends Command<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEndpoint.class);
    private Map<String, CommandExecutor<T>> commandExecutors;

    @Autowired
    public CommandEndpoint(Set<CommandExecutor<T>> cmdExecutors) {
        this.commandExecutors = cmdExecutors.stream().collect(Collectors.toMap(CommandExecutor::getHandledType,
                                                                               Function.identity()));
    }

    @StreamListener(ProcessEngineChannels.COMMAND_CONSUMER)
    public void consumeActivateProcessInstanceCmd(T cmd) {
        processCommand(cmd);
    }

    private void processCommand(T cmd) {
        CommandExecutor<T> cmdExecutor = commandExecutors.get(cmd.getCommandType().name());
        if (cmdExecutor != null) {
            cmdExecutor.execute(cmd);
            return;
        }

        LOGGER.warn(">>> No Command Found for type: " + cmd.getClass());
    }
}
