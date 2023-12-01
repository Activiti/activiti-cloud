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
package org.activiti.cloud.services.events.message;

/**
 * Internal message header key names used in messages with IntegrationContext payload type
 *
 */
public class ExecutionContextMessageHeaders {

    public static final String MESSAGE_PAYLOAD_TYPE = "messagePayloadType";
    public static final String ROOT_BUSINESS_KEY = "rootBusinessKey";
    public static final String ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";
    public static final String ROOT_PROCESS_NAME = "rootProcessName";
    public static final String ROOT_PROCESS_DEFINITION_ID = "rootProcessDefinitionId";
    public static final String ROOT_PROCESS_DEFINITION_KEY = "rootProcessDefinitionKey";
    public static final String ROOT_PROCESS_DEFINITION_VERSION = "rootProcessDefinitionVersion";
    public static final String ROOT_PROCESS_DEFINITION_NAME = "rootProcessDefinitionName";
    public static final String DEPLOYMENT_ID = "deploymentId";
    public static final String DEPLOYMENT_NAME = "deploymentName";
    public static final String DEPLOYMENT_VERSION = "deploymentVersion";
    public static final String PARENT_PROCESS_INSTANCE_ID = "parentProcessInstanceId";
    public static final String PARENT_PROCESS_INSTANCE_NAME = "parentProcessInstanceName";
}
