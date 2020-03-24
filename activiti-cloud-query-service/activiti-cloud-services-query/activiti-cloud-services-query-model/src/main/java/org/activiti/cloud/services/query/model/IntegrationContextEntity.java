package org.activiti.cloud.services.query.model;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity(name="IntegrationContext")
@Table(name="INTEGRATION_CONTEXT", indexes={
//    @Index(name="bpmn_activity_status_idx", columnList="status", unique=false),
//    @Index(name="bpmn_activity_processInstance_idx", columnList="processInstanceId", unique=false),
//    @Index(name="bpmn_activity_processInstance_elementId_idx", columnList="processInstanceId,elementId", unique=true)
})
public class IntegrationContextEntity extends ActivitiEntityMetadata {

    public static enum IntegrationContextStatus {
        INTEGRATION_REQUESTED, RESULT_RECEIVED, ERROR_RECEIVED
    }

    @Id
    private String id;

    @Convert(converter = MapOfStringObjectJsonConverter.class)
    @Column(columnDefinition="text")
    private Map<String, Object> inboundVariables = new HashMap<>();

    @Convert(converter = MapOfStringObjectJsonConverter.class)
    @Column(columnDefinition="text")
    private Map<String, Object> outBoundVariables = new HashMap<>();

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

    private String errorMessage;

    private String errorClassName;

    @Convert(converter = ListOfStackTraceElementsJsonConverter.class)
    @Column(columnDefinition="text")
    private List<StackTraceElement> stackTraceElements;

    private IntegrationContextStatus status;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private BPMNActivityEntity bpmnActivity;

    public IntegrationContextEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public IntegrationContextEntity(String serviceName,
                                    String serviceFullName,
                                    String serviceVersion,
                                    String appName,
                                    String appVersion) {
        super(serviceName,
              serviceFullName,
              serviceVersion,
              appName,
              appVersion);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public Map<String, Object> getInBoundVariables() {
        return inboundVariables;
    }

    public void setInBoundVariables(Map<String, Object> inboundVariables) {
        this.inboundVariables = inboundVariables;
    }

    public Map<String, Object> getOutBoundVariables() {
        return outBoundVariables;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getClientName() {
        return clientName;
    }


    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientType() {
        return clientType;
    }


    public void setClientType(String clientType) {
        this.clientType = clientType;
    }


    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(String parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    public Map<String, Object> getInboundVariables() {
        return inboundVariables;
    }

    public void setInboundVariables(Map<String, Object> inboundVariables) {
        this.inboundVariables = inboundVariables;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getResultDate() {
        return resultDate;
    }

    public void setResultDate(Date resultDate) {
        this.resultDate = resultDate;
    }

    public Date getErrorDate() {
        return errorDate;
    }

    public void setErrorDate(Date errorDate) {
        this.errorDate = errorDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorClassName() {
        return errorClassName;
    }

    public void setErrorClassName(String errorClassName) {
        this.errorClassName = errorClassName;
    }

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
                                               errorMessage,
                                               id,
                                               inboundVariables,
                                               outBoundVariables,
                                               parentProcessInstanceId,
                                               processDefinitionId,
                                               processDefinitionKey,
                                               processDefinitionVersion,
                                               processInstanceId,
                                               requestDate,
                                               resultDate,
                                               stackTraceElements,
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
        IntegrationContextEntity other = (IntegrationContextEntity) obj;
        return Objects.equals(businessKey, other.businessKey) &&
               Objects.equals(clientId, other.clientId) &&
               Objects.equals(clientName, other.clientName) &&
               Objects.equals(clientType, other.clientType) &&
               Objects.equals(connectorType, other.connectorType) &&
               Objects.equals(errorClassName, other.errorClassName) &&
               Objects.equals(errorDate,other.errorDate) &&
               Objects.equals(errorMessage, other.errorMessage) &&
               Objects.equals(id, other.id) &&
               Objects.equals(inboundVariables, other.inboundVariables) &&
               Objects.equals(outBoundVariables, other.outBoundVariables) &&
               Objects.equals(parentProcessInstanceId, other.parentProcessInstanceId) &&
               Objects.equals(processDefinitionId, other.processDefinitionId) &&
               Objects.equals(processDefinitionKey, other.processDefinitionKey) &&
               Objects.equals(processDefinitionVersion, other.processDefinitionVersion) &&
               Objects.equals(processInstanceId, other.processInstanceId) &&
               Objects.equals(executionId, other.executionId) &&
               Objects.equals(requestDate, other.requestDate) &&
               Objects.equals(resultDate, other.resultDate) &&
               Objects.equals(stackTraceElements, other.stackTraceElements) &&
               status == other.status;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("IntegrationContextEntity [id=")
               .append(id)
               .append(", inboundVariables=")
               .append(inboundVariables != null ? toString(inboundVariables.entrySet(), maxLen) : null)
               .append(", outBoundVariables=")
               .append(outBoundVariables != null ? toString(outBoundVariables.entrySet(), maxLen) : null)
               .append(", processInstanceId=")
               .append(processInstanceId)
               .append(", executionId=")
               .append(executionId)
               .append(", parentProcessInstanceId=")
               .append(parentProcessInstanceId)
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
               .append(", errorMessage=")
               .append(errorMessage)
               .append(", errorClassName=")
               .append(errorClassName)
               .append(", stackTraceElements=")
               .append(stackTraceElements != null ? toString(stackTraceElements, maxLen) : null)
               .append(", status=")
               .append(status)
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


    public IntegrationContextStatus getStatus() {
        return status;
    }


    public void setStatus(IntegrationContextStatus status) {
        this.status = status;
    }


    public BPMNActivityEntity getBpmnActivity() {
        return bpmnActivity;
    }


    public void setBpmnActivity(BPMNActivityEntity bpmnActivity) {
        this.bpmnActivity = bpmnActivity;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

}
