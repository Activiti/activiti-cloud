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

/**
 * Holds message header key names used in messages with JobMessage payload type
 *
 */

public final class JobMessageHeaders {

    public static final String JOB_PROCESS_INSTANCE_ID = "jobProcessInstanceId";
    public static final String JOB_PROCESS_DEFINITION_ID = "jobProcessDefinitionId";
    public static final String JOB_EXECUTION_ID = "jobExecutionId";
    public static final String JOB_TYPE = "jobType";
    public static final String JOB_ID = "jobId";
    public static final String JOB_DUE_DATE = "jobDueDate";
    public static final String JOB_RETRIES = "jobRetries";
    public static final String JOB_EXCEPTION_MESSAGE = "jobExceptionMessage";
    public static final String JOB_HANDLER_TYPE = "jobHandlerType";
    public static final String JOB_HANDLER_CONFIGURATION = "jobHandlerConfiguration";
}
