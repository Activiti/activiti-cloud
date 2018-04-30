/*
 * Copyright 2018 Alfresco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.events;




public class ProcessStartedEventImpl  extends AbstractProcessEngineEvent implements ProcessStartedEvent {

    private String nestedProcessDefinitionId;
    private String nestedProcessInstanceId;

    public ProcessStartedEventImpl() {
    }

    public ProcessStartedEventImpl(String appName, String appVersion, String serviceName, String serviceFullName, String serviceType, String serviceVersion,

                                   String executionId,
                                   String processDefinitionId,
                                   String processInstanceId,
                                   String nestedProcessDefinitionId,
                                   String nestedProcessInstanceId) {
        super(appName,appVersion,serviceName,serviceFullName,serviceType,serviceVersion,

              executionId,
              processDefinitionId,
              processInstanceId);
        this.nestedProcessDefinitionId = nestedProcessDefinitionId;
        this.nestedProcessInstanceId = nestedProcessInstanceId;
    }

    @Override
    public String getNestedProcessInstanceId() {
        return nestedProcessInstanceId;
    }

    @Override
    public String getNestedProcessDefinitionId() {
        return nestedProcessDefinitionId;
    }

    @Override
    public String getEventType() {
        return "ProcessStartedEvent";
    }
}
