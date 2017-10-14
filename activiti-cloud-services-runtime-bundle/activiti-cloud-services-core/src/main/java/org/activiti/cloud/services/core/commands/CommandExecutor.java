package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.Command;

public interface CommandExecutor<T extends Command> {

    Class getHandledType();

    void execute(T cmd);
}
