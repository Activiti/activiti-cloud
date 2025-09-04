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
package org.activiti.cloud.services.job.executor;

import org.activiti.engine.cfg.ProcessEngineConfigurator;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.SmartLifecycle;

public class MessageBasedJobManagerConfigurator implements ProcessEngineConfigurator, SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MessageBasedJobManagerConfigurator.class);

    private static final String MESSAGE_BASED_JOB_MANAGER = "messageBasedJobManager";

    private final MessageBasedJobManagerFactory messageBasedJobManagerFactory;
    private final ConfigurableListableBeanFactory beanFactory;

    private ProcessEngineConfigurationImpl configuration;

    private boolean running = false;

    public MessageBasedJobManagerConfigurator(
        ConfigurableListableBeanFactory beanFactory,
        MessageBasedJobManagerFactory messageBasedJobManagerFactory
    ) {
        this.messageBasedJobManagerFactory = messageBasedJobManagerFactory;
        this.beanFactory = beanFactory;
    }

    protected MessageBasedJobManager createMessageBasedJobManager(ProcessEngineConfigurationImpl configuration) {
        MessageBasedJobManager instance = messageBasedJobManagerFactory.create(configuration);

        return registerBean(MESSAGE_BASED_JOB_MANAGER, instance);
    }

    /**
     * Configures MessageBasedJobManager
     */
    @Override
    public void beforeInit(ProcessEngineConfigurationImpl configuration) {
        var messageBasedJobManager = createMessageBasedJobManager(configuration);

        // Let's manage async executor lifecycle manually on start/stop
        configuration.setAsyncExecutorActivate(false);
        configuration.setAsyncExecutorMessageQueueMode(true);
        configuration.setJobManager(messageBasedJobManager);

        logger.info("Configured message based job manager class: {}", messageBasedJobManager.getClass());
    }

    /**
     * Configures input channel
     */
    @Override
    public void configure(ProcessEngineConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void start() {
        // Now start async executor
        if (!configuration.getAsyncExecutor().isActive()) {
            configuration.getAsyncExecutor().start();
        }

        running = true;
    }

    @Override
    public void stop() {
        try {
            // Let's gracefully shutdown executor
            if (configuration.getAsyncExecutor().isActive()) {
                configuration.getAsyncExecutor().shutdown();
            }
        } finally {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @SuppressWarnings("unchecked")
    protected <T> T registerBean(String name, T bean) {
        beanFactory.registerSingleton(name, bean);

        return (T) beanFactory.initializeBean(bean, name);
    }
}
