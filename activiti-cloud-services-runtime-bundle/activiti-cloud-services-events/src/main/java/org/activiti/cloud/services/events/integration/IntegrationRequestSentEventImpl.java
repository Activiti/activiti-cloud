/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.integration;




public class IntegrationRequestSentEventImpl extends BaseIntegrationEventImpl implements IntegrationRequestSentEvent {

    //used to deserialize from json
    public IntegrationRequestSentEventImpl() {
    }

    public IntegrationRequestSentEventImpl(String appName, String appVersion, String serviceName, String serviceFullName, String serviceType, String serviceVersion,

                                           String executionId,
                                           String processDefinitionId,
                                           String processInstanceId,
                                           String integrationContextId,
                                           String flowNodeId) {
        super(appName,appVersion,serviceName,serviceFullName,serviceType,serviceVersion,

              executionId,
              processDefinitionId,
              processInstanceId,
              integrationContextId,
              flowNodeId);
    }

    @Override
    public String getEventType() {
        return "IntegrationRequestSentEvent";
    }
}
