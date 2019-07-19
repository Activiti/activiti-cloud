package org.activiti.cloud.services.job.executor;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.messaging.MessageHandler;

public interface JobMessageHandlerFactory {

    MessageHandler create(ProcessEngineConfigurationImpl configuration);
}
