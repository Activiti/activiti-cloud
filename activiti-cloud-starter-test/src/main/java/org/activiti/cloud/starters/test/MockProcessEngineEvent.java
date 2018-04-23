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

package org.activiti.cloud.starters.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.activiti.cloud.services.api.events.ProcessEngineEvent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MockProcessEngineEvent implements ProcessEngineEvent {

    private Long timestamp;
    private String eventType;
    private String executionId;
    private String processDefinitionId;
    private String processInstanceId;
    private String appName;
    private String appVersion;
    private String serviceName;
    private String serviceFullName;
    private String serviceType;
    private String serviceVersion;

    public MockProcessEngineEvent() {
    }

    public MockProcessEngineEvent(Long timestamp,
                                  String eventType) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.serviceName = "mock-app-name";
        this.serviceFullName = "mock-app-name";
        this.serviceType = "runtime-bundle";
        this.serviceVersion = "1";
    }

    public MockProcessEngineEvent(Long timestamp,
                                  String eventType,
                                  String executionId,
                                  String processDefinitionId,
                                  String processInstanceId) {
        this(timestamp,
             eventType);
        this.executionId = executionId;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
    }

    public static ProcessEngineEvent[] aProcessCreatedEvent(Long timestamp,
                                                            String executionId,
                                                            String processDefinitionId,
                                                            String processInstanceId) {
        ProcessEngineEvent[] events = {new MockProcessEngineEvent(timestamp,
                                                                  "ProcessCreatedEvent",
                                                                  executionId,
                                                                  processDefinitionId,
                                                                  processInstanceId)};
        return events;
    }

    public static ProcessEngineEvent[] aProcessStartedEvent(Long timestamp,
                                                            String executionId,
                                                            String processDefinitionId,
                                                            String processInstanceId) {
        ProcessEngineEvent[] events = {new MockProcessEngineEvent(timestamp,
                                                                  "ProcessStartedEvent",
                                                                  executionId,
                                                                  processDefinitionId,
                                                                  processInstanceId)};
        return events;
    }

    public static ProcessEngineEvent[] aProcessCompletedEvent(Long timestamp,
                                                              String executionId,
                                                              String processDefinitionId,
                                                              String processInstanceId) {
        ProcessEngineEvent[] events = {new MockProcessEngineEvent(timestamp,
                                                                  "ProcessCompletedEvent",
                                                                  executionId,
                                                                  processDefinitionId,
                                                                  processInstanceId)};
        return events;
    }

    @Override
    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getServiceFullName() {
        return serviceFullName;
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    @Override
    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

}
