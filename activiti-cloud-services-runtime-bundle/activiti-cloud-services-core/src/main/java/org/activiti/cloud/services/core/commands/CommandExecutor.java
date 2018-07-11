package org.activiti.cloud.services.core.commands;

import org.activiti.runtime.api.cmd.Command;

public interface CommandExecutor<T extends Command<?>> {

    String getHandledType();

    void execute(T cmd);
}
