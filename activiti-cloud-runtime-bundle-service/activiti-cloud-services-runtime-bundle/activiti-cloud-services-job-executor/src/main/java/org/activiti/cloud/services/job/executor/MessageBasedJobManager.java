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

import java.util.Date;
import org.activiti.engine.impl.asyncexecutor.DefaultJobManager;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;

public class MessageBasedJobManager extends DefaultJobManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageBasedJobManager.class);

    private final BindingServiceProperties bindingServiceProperties;
    private final JobMessageProducer jobMessageProducer;

    private String inputChannelName = MessageBasedJobManagerChannelsConstants.INPUT;
    private String outputChannelName = MessageBasedJobManagerChannelsConstants.OUTPUT;

    public MessageBasedJobManager(
        ProcessEngineConfigurationImpl processEngineConfiguration,
        BindingServiceProperties bindingServiceProperties,
        JobMessageProducer jobMessageProducer
    ) {
        super(processEngineConfiguration);
        this.bindingServiceProperties = bindingServiceProperties;
        this.jobMessageProducer = jobMessageProducer;
    }

    @Override
    protected void triggerExecutorIfNeeded(final JobEntity jobEntity) {
        logger.debug("triggerExecutorIfNeeded for job: {}", jobEntity);

        sendMessage(jobEntity);
    }

    @Override
    public void unacquire(final Job job) {
        logger.debug("unacquire job: {}", job);

        if (job instanceof JobEntity) {
            JobEntity jobEntity = (JobEntity) job;

            // When unacquiring, we up the lock time again., so that it isn't cleared by the reset expired thread.
            jobEntity.setLockExpirationTime(
                new Date(
                    processEngineConfiguration.getClock().getCurrentTime().getTime() +
                    processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis()
                )
            );
        }

        sendMessage(job);
    }

    public BindingProperties getBindingProperties() {
        return bindingServiceProperties.getBindingProperties(MessageBasedJobManagerChannelsConstants.INPUT);
    }

    public String getOutputChannelName() {
        return outputChannelName;
    }

    public String getInputChannelName() {
        return inputChannelName;
    }

    public void sendMessage(final Job job) {
        logger.debug("sendMessage for job: {}", job);

        jobMessageProducer.sendMessage(getOutputChannelName(), job);
    }
}
