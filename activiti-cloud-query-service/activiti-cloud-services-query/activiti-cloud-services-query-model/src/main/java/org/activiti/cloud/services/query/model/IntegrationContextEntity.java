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
package org.activiti.cloud.services.query.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import jakarta.persistence.*;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name = "IntegrationContext")
@Table(
    name = "INTEGRATION_CONTEXT",
    indexes = {
        @Index(name = "integration_context_status_idx", columnList = "status", unique = false),
        @Index(name = "integration_context_processInstance_idx", columnList = "processInstanceId", unique = false),
        @Index(
            name = "integration_context_processInstance_elementId_idx",
            columnList = "processInstanceId,clientId,executionId",
            unique = true
        ),
    }
)
@DynamicInsert
@DynamicUpdate
public class IntegrationContextEntity extends ActivitiEntityMetadata implements CloudIntegrationContext {

    public static final int ERROR_MESSAGE_LENGTH = 255;

    @Id
    private String id;

    @Convert(converter = MapOfStringObjectJsonConverter.class)
    @Column(columnDefinition = "text", name = "inbound_variables")
    private Map<String, Object> inBoundVariables = new HashMap<>();

    @Convert(converter = MapOfStringObjectJsonConverter.class)
    @Column(columnDefinition = "text")
    private Map<String, Object> outBoundVariables = new HashMap<>();

    private String rootProcessInstanceId;
    private String processInstanceId;
    private String parentProcessInstanceId;
    private String executionId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private Integer processDefinitionVersion;
    private String businessKey;
    private String clientId;
    private String clientName;
    private String clientType;
    private String connectorType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date requestDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date resultDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date errorDate;

    private String errorCode;

    @Column(length = ERROR_MESSAGE_LENGTH)
    private String errorMessage;

    private String errorClassName;

    @Convert(converter = ListOfStackTraceElementsJsonConverter.class)
    @Column(columnDefinition = "text")
    private List<StackTraceElement> stackTraceElements;

    @JsonFormat(shape = Shape.STRING)
    @Enumerated(EnumType.STRING)
    private IntegrationContextStatus status;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @MapsId
    @JoinColumn(name = "id")
    private ServiceTaskEntity serviceTask;

    public IntegrationContextEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public IntegrationContextEntity(
        String serviceName,
        String serviceFullName,
        String serviceVersion,
        String appName,
        String appVersion
    ) {
        super(serviceName, serviceFullName, serviceVersion, appName, appVersion);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getRootProcessInstanceId() {
        return this.rootProcessInstanceId;
    }

    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    @Override
    public Map<String, Object> getInBoundVariables() {
        return inBoundVariables;
    }

    public void setInBoundVariables(Map<String, Object> inboundVariables) {
        this.inBoundVariables = inboundVariables;
    }

    @Override
    public Map<String, Object> getOutBoundVariables() {
        return outBoundVariables;
    }

    @Override
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    @Deprecated
    @JsonIgnore
    public Map<String, Object> getInboundVariables() {
        return getInboundVariables();
    }

    @Deprecated
    public void setInboundVariables(Map<String, Object> inboundVariables) {
        setInBoundVariables(inboundVariables);
    }

    @Override
    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    @Override
    public Date getResultDate() {
        return resultDate;
    }

    public void setResultDate(Date resultDate) {
        this.resultDate = resultDate;
    }

    @Override
    public Date getErrorDate() {
        return errorDate;
    }

    public void setErrorDate(Date errorDate) {
        this.errorDate = errorDate;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = StringUtils.truncate(errorMessage, ERROR_MESSAGE_LENGTH);
    }

    @Override
    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

    @Override
    public List<StackTraceElement> getStackTraceElements() {
        return stackTraceElements;
    }

    public void setStackTraceElements(List<StackTraceElement> stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    public void setOutBoundVariables(Map<String, Object> outBoundVariables) {
        this.outBoundVariables = outBoundVariables;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        IntegrationContextEntity other = (IntegrationContextEntity) obj;

        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public IntegrationContextStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationContextStatus status) {
        this.status = status;
    }

    public ServiceTaskEntity getServiceTask() {
        return serviceTask;
    }

    public void setServiceTask(ServiceTaskEntity serviceTask) {
        if (serviceTask == null) {
            if (this.serviceTask != null) {
                this.serviceTask.setIntegrationContext(null);
            }
        } else {
            serviceTask.setIntegrationContext(this);
        }

        this.serviceTask = serviceTask;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public void addOutBoundVariable(String name, Object value) {
        outBoundVariables.put(name, value);
    }

    @Override
    public void addOutBoundVariables(Map<String, Object> variables) {
        outBoundVariables.putAll(variables);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInBoundVariable(String name) {
        return Optional.ofNullable(inBoundVariables).map(it -> (T) inBoundVariables.get(name)).orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInBoundVariable(String name, Class<T> type) {
        return Optional.ofNullable(inBoundVariables).map(it -> (T) inBoundVariables.get(name)).orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOutBoundVariable(String name) {
        return Optional.ofNullable(outBoundVariables).map(it -> (T) it.get(name)).orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOutBoundVariable(String name, Class<T> type) {
        return Optional.ofNullable(outBoundVariables).map(it -> (T) it.get(name)).orElse(null);
    }

    public static class IdBuilderHelper {

        public static String from(IntegrationContext integrationContext) {
            return new StringBuilder()
                .append(integrationContext.getProcessInstanceId())
                .append(":")
                .append(integrationContext.getClientId())
                .append(":")
                .append(integrationContext.getExecutionId())
                .toString();
        }
    }
}
