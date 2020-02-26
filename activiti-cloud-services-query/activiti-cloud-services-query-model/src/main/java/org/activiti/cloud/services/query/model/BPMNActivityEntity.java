package org.activiti.cloud.services.query.model;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.springframework.format.annotation.DateTimeFormat;

@Entity(name="BPMNActivity")
@Table(name="BPMN_ACTIVITY", indexes={
    @Index(name="bpmn_activity_status_idx", columnList="status", unique=false),
    @Index(name="bpmn_activity_processInstance_idx", columnList="processInstanceId", unique=false),
    @Index(name="bpmn_activity_processInstance_elementId_idx", columnList="processInstanceId,elementId", unique=true)
})
public class BPMNActivityEntity extends ActivitiEntityMetadata implements CloudBPMNActivity {
    
    public static enum BPMNActivityStatus {
        STARTED, COMPLETED, CANCELLED
    }
    
    /** The unique identifier of this historic activity instance. */
    @Id
    private String id;

    /** The unique identifier of the activity in the process */
    private String elementId;       
    
    /** The display name for the activity */
    private String activityName;
    
    /** The XML tag of the activity as in the process file */
    private String activityType;

    /** The associated process instance id */
    private String processInstanceId;

    /** The associated process definition id */
    private String processDefinitionId;
    
    /** The current state of activity */
    @Enumerated(EnumType.STRING)
    private BPMNActivityStatus status;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date startedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)    
    private Date completedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)    
    private Date cancelledDate;
    
    /** The associated process definition key of the activity as in the process file */
    private String processDefinitionKey;

    /** The associated deployed process definition version of the activity */
    private Integer processDefinitionVersion;

    /** The associated business key of the activity as in the process instance */
    private String businessKey;    
    
    public BPMNActivityEntity() {}

    public BPMNActivityEntity(String serviceName,
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

    @Override
    public String getElementId() {
        return elementId;
    };

    public String getActivityName() {
        return activityName;
    };

    public String getActivityType() {
        return activityType;
    }; 

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public BPMNActivityStatus getStatus() {
        return status;
    }

    public void setStatus(BPMNActivityStatus status) {
        this.status = status;
    }

    
    public Date getStartedDate() {
        return startedDate;
    }

    
    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate;
    }

    
    public Date getCompletedDate() {
        return completedDate;
    }

    
    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    
    public void setId(String id) {
        this.id = id;
    }

    
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    
    public Date getCancelledDate() {
        return cancelledDate;
    }

    
    public void setCancelledDate(Date cancelledDate) {
        this.cancelledDate = cancelledDate;
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

    
    public String getBusinessKey() {
        return businessKey;
    }

    
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(activityName,
                                               activityType,
                                               businessKey,
                                               cancelledDate,
                                               completedDate,
                                               elementId,
                                               id,
                                               processDefinitionId,
                                               processDefinitionKey,
                                               processDefinitionVersion,
                                               processInstanceId,
                                               startedDate,
                                               status);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        BPMNActivityEntity other = (BPMNActivityEntity) obj;
        return Objects.equals(activityName, other.activityName) && 
               Objects.equals(activityType, other.activityType) && 
               Objects.equals(businessKey, other.businessKey) && 
               Objects.equals(cancelledDate, other.cancelledDate) && 
               Objects.equals(completedDate, other.completedDate) && 
               Objects.equals(elementId, other.elementId) && 
               Objects.equals(id, other.id) &&
               Objects.equals(processDefinitionId, other.processDefinitionId) && 
               Objects.equals(processDefinitionKey, other.processDefinitionKey) && 
               Objects.equals(processDefinitionVersion, other.processDefinitionVersion) && 
               Objects.equals(processInstanceId, other.processInstanceId) && 
               Objects.equals(startedDate, other.startedDate) && 
               status == other.status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BPMNActivityEntity [id=");
        builder.append(id);
        builder.append(", elementId=");
        builder.append(elementId);
        builder.append(", activityName=");
        builder.append(activityName);
        builder.append(", activityType=");
        builder.append(activityType);
        builder.append(", processInstanceId=");
        builder.append(processInstanceId);
        builder.append(", processDefinitionId=");
        builder.append(processDefinitionId);
        builder.append(", status=");
        builder.append(status);
        builder.append(", startedDate=");
        builder.append(startedDate);
        builder.append(", completedDate=");
        builder.append(completedDate);
        builder.append(", cancelledDate=");
        builder.append(cancelledDate);
        builder.append(", processDefinitionKey=");
        builder.append(processDefinitionKey);
        builder.append(", processDefinitionVersion=");
        builder.append(processDefinitionVersion);
        builder.append(", businessKey=");
        builder.append(businessKey);
        builder.append("]");
        return builder.toString();
    }

    

}
