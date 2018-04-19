package org.activiti.cloud.services.audit.mongo;

import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;

public class TestProcessEngineEventDocument extends ProcessEngineEventDocument{
    //making setters visible only for testing purposes
    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    public void setTimestamp(Long timestamp) {
        super.setTimestamp(timestamp);
    }

    @Override
    public void setEventType(String eventType) {
        super.setEventType(eventType);
    }

    @Override
    public void setExecutionId(String executionId) {
        super.setExecutionId(executionId);
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        super.setProcessDefinitionId(processDefinitionId);
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        super.setProcessInstanceId(processInstanceId);
    }

    @Override
    public void setApplicationName(String applicationName) {
        super.setApplicationName(applicationName);
    }
}
