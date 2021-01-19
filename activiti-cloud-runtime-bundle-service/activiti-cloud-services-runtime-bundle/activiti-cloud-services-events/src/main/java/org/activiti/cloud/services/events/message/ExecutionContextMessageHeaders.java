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
class ExecutionContextMessageHeaders {

    public final static String MESSAGE_PAYLOAD_TYPE = "messagePayloadType";
    public final static String ROOT_BUSINESS_KEY = "rootBusinessKey";
    public final static String ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";
    public final static String ROOT_PROCESS_NAME = "rootProcessName";
    public final static String ROOT_PROCESS_DEFINITION_ID = "rootProcessDefinitionId";
    public final static String ROOT_PROCESS_DEFINITION_KEY = "rootProcessDefinitionKey";
    public final static String ROOT_PROCESS_DEFINITION_VERSION = "rootProcessDefinitionVersion";
    public final static String ROOT_PROCESS_DEFINITION_NAME = "rootProcessDefinitionName";
    public final static String DEPLOYMENT_ID = "deploymentId";
    public final static String DEPLOYMENT_NAME = "deploymentName";
    public final static String PARENT_PROCESS_INSTANCE_ID = "parentProcessInstanceId";
    public final static String PARENT_PROCESS_INSTANCE_NAME = "parentProcessInstanceName";
    public final static String APP_VERSION = "appVersion";

}