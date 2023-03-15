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
package org.activiti.services.connectors.message;

/**
 * Holds message header key names used in messages with IntegrationContext payload type
 *
 */
class IntegrationContextMessageHeaders {

    public static final String CONNECTOR_TYPE = "connectorType";
    public static final String BUSINESS_KEY = "businessKey";
    public static final String INTEGRATION_CONTEXT_ID = "integrationContextId";
    public static final String ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String PARENT_PROCESS_INSTANCE_ID = "parentProcessInstanceId";
    public static final String PROCESS_DEFINITION_ID = "processDefinitionId";
    public static final String PROCESS_DEFINITION_KEY = "processDefinitionKey";
    public static final String EXECUTION_ID = "executionId";
    public static final String PROCESS_DEFINITION_VERSION = "processDefinitionVersion";
    public static final String APP_VERSION = "appVersion";
}
