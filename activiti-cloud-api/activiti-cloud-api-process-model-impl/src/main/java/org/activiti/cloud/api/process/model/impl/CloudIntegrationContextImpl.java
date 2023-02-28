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
package org.activiti.cloud.api.process.model.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.activiti.cloud.api.model.shared.impl.CloudRuntimeEntityImpl;
import org.activiti.cloud.api.process.model.CloudIntegrationContext;

public class CloudIntegrationContextImpl extends CloudRuntimeEntityImpl implements CloudIntegrationContext {

    private String id;
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
    private Date requestDate;
    private Date resultDate;
    private Date errorDate;
    private String errorCode;
    private String errorMessage;
    private String errorClassName;
    private String stackTrace;
    private IntegrationContextStatus status;
    private Map<String, Object> inBoundVariables = new HashMap<>();
    private Map<String, Object> outBoundVariables = new HashMap<>();

    public CloudIntegrationContextImpl() {}

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Map<String, Object> getInBoundVariables() {
        return inBoundVariables;
    }

    public void setInBoundVariables(Map<String, Object> inBoundVariables) {
        this.inBoundVariables = inBoundVariables;
    }

    @Override
    public Map<String, Object> getOutBoundVariables() {
        return outBoundVariables;
    }

    public void setOutBoundVariables(Map<String, Object> outBoundVariables) {
        this.outBoundVariables = outBoundVariables;
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
    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
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
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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
    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
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
        this.errorMessage = errorMessage;
    }

    @Override
    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public IntegrationContextStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationContextStatus status) {
        this.status = status;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public void addOutBoundVariable(String name,
                                    Object value) {
        outBoundVariables.put(name, value);
    }
    @Override
    public void addOutBoundVariables(Map<String, Object> variables) {
        outBoundVariables.putAll(variables);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInBoundVariable(String name) {
        return Optional.ofNullable(inBoundVariables)
                       .map(it -> (T) inBoundVariables.get(name))
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInBoundVariable(String name, Class<T> type) {
        return Optional.ofNullable(inBoundVariables)
                       .map(it -> (T) inBoundVariables.get(name))
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOutBoundVariable(String name) {
        return Optional.ofNullable(outBoundVariables)
                       .map(it -> (T) it.get(name))
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOutBoundVariable(String name, Class<T> type) {
        return Optional.ofNullable(outBoundVariables)
                       .map(it -> (T) it.get(name))
                       .orElse(null);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(businessKey,
                                               clientId,
                                               clientName,
                                               clientType,
                                               connectorType,
                                               errorClassName,
                                               errorDate,
                                               errorCode,
                                               errorMessage,
                                               executionId,
                                               id,
                                               inBoundVariables,
                                               outBoundVariables,
                                               parentProcessInstanceId,
                                               rootProcessInstanceId,
                                               processDefinitionId,
                                               processDefinitionKey,
                                               processDefinitionVersion,
                                               processInstanceId,
                                               requestDate,
                                               resultDate,
                                               stackTrace,
                                               status);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudIntegrationContextImpl other = (CloudIntegrationContextImpl) obj;
        return Objects.equals(businessKey, other.businessKey) &&
               Objects.equals(clientId, other.clientId) &&
               Objects.equals(clientName, other.clientName) &&
               Objects.equals(clientType, other.clientType) &&
               Objects.equals(connectorType, other.connectorType) &&
               Objects.equals(errorClassName, other.errorClassName) &&
               Objects.equals(errorDate, other.errorDate) &&
               Objects.equals(errorCode, other.errorCode) &&
               Objects.equals(errorMessage, other.errorMessage) &&
               Objects.equals(executionId, other.executionId) &&
               Objects.equals(id, other.id) &&
               Objects.equals(inBoundVariables, other.inBoundVariables) &&
               Objects.equals(outBoundVariables, other.outBoundVariables) &&
               Objects.equals(parentProcessInstanceId, other.parentProcessInstanceId) &&
               Objects.equals(processDefinitionId, other.processDefinitionId) &&
               Objects.equals(processDefinitionKey, other.processDefinitionKey) &&
               Objects.equals(processDefinitionVersion, other.processDefinitionVersion) &&
               Objects.equals(rootProcessInstanceId, other.rootProcessInstanceId) &&
               Objects.equals(processInstanceId, other.processInstanceId) &&
               Objects.equals(requestDate, other.requestDate) &&
               Objects.equals(resultDate, other.resultDate) &&
               Objects.equals(stackTrace, other.stackTrace) &&
               status == other.status;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("CloudIntegrationContextImpl [id=")
               .append(id)
               .append(", rootProcessInstanceId=")
               .append(rootProcessInstanceId)
               .append(", processInstanceId=")
               .append(processInstanceId)
               .append(", parentProcessInstanceId=")
               .append(parentProcessInstanceId)
               .append(", executionId=")
               .append(executionId)
               .append(", processDefinitionId=")
               .append(processDefinitionId)
               .append(", processDefinitionKey=")
               .append(processDefinitionKey)
               .append(", processDefinitionVersion=")
               .append(processDefinitionVersion)
               .append(", businessKey=")
               .append(businessKey)
               .append(", clientId=")
               .append(clientId)
               .append(", clientName=")
               .append(clientName)
               .append(", clientType=")
               .append(clientType)
               .append(", connectorType=")
               .append(connectorType)
               .append(", requestDate=")
               .append(requestDate)
               .append(", resultDate=")
               .append(resultDate)
               .append(", errorDate=")
               .append(errorDate)
               .append(", errorCode=")
               .append(errorCode)
               .append(", errorMessage=")
               .append(errorMessage)
               .append(", errorClassName=")
               .append(errorClassName)
               .append(", stackTrace=")
               .append(stackTrace)
               .append(", status=")
               .append(status)
               .append(", inBoundVariables=")
               .append(inBoundVariables != null ? toString(inBoundVariables.entrySet(), maxLen) : null)
               .append(", outBoundVariables=")
               .append(outBoundVariables != null ? toString(outBoundVariables.entrySet(), maxLen) : null)
               .append("]");
        return builder.toString();
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }


}
