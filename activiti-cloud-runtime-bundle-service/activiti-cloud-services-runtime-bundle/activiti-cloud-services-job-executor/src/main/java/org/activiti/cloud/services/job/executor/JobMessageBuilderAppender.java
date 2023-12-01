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

import org.activiti.cloud.services.events.message.MessageBuilderAppender;
import org.activiti.engine.runtime.Job;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class JobMessageBuilderAppender implements MessageBuilderAppender {

    private final Job job;

    public JobMessageBuilderAppender(Job job) {
        Assert.notNull(job, "job must not be null");

        this.job = job;
    }

    @Override
    public <P> MessageBuilder<P> apply(MessageBuilder<P> request) {
        Assert.notNull(request, "request must not be null");

        return request
            .setHeader(JobMessageHeaders.JOB_TYPE, job.getJobType())
            .setHeader(JobMessageHeaders.JOB_PROCESS_DEFINITION_ID, job.getProcessDefinitionId())
            .setHeader(JobMessageHeaders.JOB_DUE_DATE, job.getDuedate())
            .setHeader(JobMessageHeaders.JOB_PROCESS_INSTANCE_ID, job.getProcessInstanceId())
            .setHeader(JobMessageHeaders.JOB_EXECUTION_ID, job.getExecutionId())
            .setHeader(JobMessageHeaders.JOB_ID, job.getId())
            .setHeader(JobMessageHeaders.JOB_RETRIES, job.getRetries())
            .setHeader(JobMessageHeaders.JOB_EXECUTION_ID, job.getExecutionId())
            .setHeader(JobMessageHeaders.JOB_EXCEPTION_MESSAGE, job.getExceptionMessage())
            .setHeader(JobMessageHeaders.JOB_HANDLER_TYPE, job.getJobHandlerType())
            .setHeader(JobMessageHeaders.JOB_HANDLER_CONFIGURATION, job.getJobHandlerConfiguration());
    }
}
