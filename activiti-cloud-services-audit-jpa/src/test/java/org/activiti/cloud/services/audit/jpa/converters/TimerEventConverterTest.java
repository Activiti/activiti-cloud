/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.audit.jpa.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.TimerPayload;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerCancelledEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerExecutedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFailedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerRetriesDecrementedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.services.audit.jpa.events.TimerAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerCancelledAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerExecutedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerFailedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerFiredAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerRetriesDecrementedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.TimerScheduledAuditEventEntity;
import org.junit.Test;

public class TimerEventConverterTest {

    private TimerFiredEventConverter eventConverterTimerFired = new TimerFiredEventConverter(new EventContextInfoAppender());
    private TimerScheduledEventConverter eventConverterTimerScheduled = new TimerScheduledEventConverter(new EventContextInfoAppender());
    private TimerCancelledEventConverter eventConverterTimerCancelled = new TimerCancelledEventConverter(new EventContextInfoAppender());
    private TimerExecutedEventConverter eventConverterTimerExecuted = new TimerExecutedEventConverter(new EventContextInfoAppender());
    private TimerFailedEventConverter eventConverterTimerFailed = new TimerFailedEventConverter(new EventContextInfoAppender());
    private TimerRetriesDecrementedEventConverter eventConverterTimerRetriesDecremented = new TimerRetriesDecrementedEventConverter(new EventContextInfoAppender());
    
    
    
    @Test
    public void checkConvertToEntityTimerFiredEvent() {
        //given
        CloudBPMNTimerFiredEventImpl event = createTimerFiredEvent();
           
        //when
        TimerFiredAuditEventEntity auditEventEntity = (TimerFiredAuditEventEntity) eventConverterTimerFired.convertToEntity(event);
     
        //then
        checkCloudAuditEvententity(auditEventEntity, event);
    }
    
    @Test
    public void checkConvertToEntityTimerScheduledEvent() {
        //given
        CloudBPMNTimerScheduledEventImpl event = createTimerScheduledEvent();
           
        //when
        TimerScheduledAuditEventEntity auditEventEntity = (TimerScheduledAuditEventEntity) eventConverterTimerScheduled.convertToEntity(event);
     
        //then
        checkCloudAuditEvententity(auditEventEntity, event);
    }
    
    @Test
    public void checkConvertToEntityTimerCancelledEvent() {
        //given
        CloudBPMNTimerCancelledEventImpl event = createTimerCancelledEvent();
           
        //when
        TimerCancelledAuditEventEntity auditEventEntity = (TimerCancelledAuditEventEntity) eventConverterTimerCancelled.convertToEntity(event);
     
        //then
        checkCloudAuditEvententity(auditEventEntity, event);
    }
    
    @Test
    public void checkConvertToEntityTimerExecutedEvent() {
        //given
        CloudBPMNTimerExecutedEventImpl event = createTimerExecutedEvent();
           
        //when
        TimerExecutedAuditEventEntity auditEventEntity = (TimerExecutedAuditEventEntity) eventConverterTimerExecuted.convertToEntity(event);
     
        //then
        checkCloudAuditEvententity(auditEventEntity, event);
    }
    
    @Test
    public void checkConvertToEntityTimerRetriesDecrementedEvent() {
        //given
        CloudBPMNTimerRetriesDecrementedEventImpl event = createTimerRetriesDecrementedEvent();
           
        //when
        TimerRetriesDecrementedAuditEventEntity auditEventEntity = (TimerRetriesDecrementedAuditEventEntity) eventConverterTimerRetriesDecremented.convertToEntity(event);
     
        //then
        checkCloudAuditEvententity(auditEventEntity, event);
    }
    
    @Test
    public void checkConvertToEntityTimerFailedEvent() {
        //given
        CloudBPMNTimerFailedEventImpl event = createTimerFailedEvent();
           
        //when
        TimerFailedAuditEventEntity auditEventEntity = (TimerFailedAuditEventEntity) eventConverterTimerFailed.convertToEntity(event);
     
        //then
        checkCloudAuditEvententity(auditEventEntity, event);
    }
    
    @Test
    public void checkConvertToAPITimerFiredEvent() {
        //given
        TimerFiredAuditEventEntity auditEventEntity = (TimerFiredAuditEventEntity) eventConverterTimerFired.convertToEntity(createTimerFiredEvent());
        
        //when
        CloudBPMNTimerFiredEventImpl cloudEvent= (CloudBPMNTimerFiredEventImpl) eventConverterTimerFired.convertToAPI(auditEventEntity);
        
        checkCloudAuditEvententity(auditEventEntity, cloudEvent);
    }
    
    @Test
    public void checkConvertToAPITimerScheduledEvent() {
        //given
        TimerScheduledAuditEventEntity auditEventEntity = (TimerScheduledAuditEventEntity) eventConverterTimerScheduled.convertToEntity(createTimerScheduledEvent());
        
        //when
        CloudBPMNTimerScheduledEventImpl cloudEvent= (CloudBPMNTimerScheduledEventImpl) eventConverterTimerScheduled.convertToAPI(auditEventEntity);
        
        checkCloudAuditEvententity(auditEventEntity, cloudEvent);       
    }
    
    @Test
    public void checkConvertToAPITimerCancelledEvent() {
        //given
        TimerCancelledAuditEventEntity auditEventEntity = (TimerCancelledAuditEventEntity) eventConverterTimerCancelled.convertToEntity(createTimerCancelledEvent());
        
        //when
        CloudBPMNTimerCancelledEventImpl cloudEvent= (CloudBPMNTimerCancelledEventImpl) eventConverterTimerCancelled.convertToAPI(auditEventEntity);
        
        checkCloudAuditEvententity(auditEventEntity, cloudEvent);       
    }
    
    @Test
    public void checkConvertToAPITimerExecutedEvent() {
        //given
        TimerExecutedAuditEventEntity auditEventEntity = (TimerExecutedAuditEventEntity) eventConverterTimerExecuted.convertToEntity(createTimerExecutedEvent());
        
        //when
        CloudBPMNTimerExecutedEventImpl cloudEvent= (CloudBPMNTimerExecutedEventImpl) eventConverterTimerExecuted.convertToAPI(auditEventEntity);
        
        checkCloudAuditEvententity(auditEventEntity, cloudEvent);       
    }
    
    @Test
    public void checkConvertToAPITimerFailedEvent() {
        //given
        TimerFailedAuditEventEntity auditEventEntity = (TimerFailedAuditEventEntity) eventConverterTimerFailed.convertToEntity(createTimerFailedEvent());
        
        //when
        CloudBPMNTimerFailedEventImpl cloudEvent= (CloudBPMNTimerFailedEventImpl) eventConverterTimerFailed.convertToAPI(auditEventEntity);
        
        checkCloudAuditEvententity(auditEventEntity, cloudEvent);       
    }
    
    @Test
    public void checkConvertToAPITimerRetriesDecrementedEvent() {
        //given
        TimerRetriesDecrementedAuditEventEntity auditEventEntity = (TimerRetriesDecrementedAuditEventEntity) eventConverterTimerRetriesDecremented.convertToEntity(createTimerRetriesDecrementedEvent());
        
        //when
        CloudBPMNTimerRetriesDecrementedEventImpl cloudEvent= (CloudBPMNTimerRetriesDecrementedEventImpl) eventConverterTimerRetriesDecremented.convertToAPI(auditEventEntity);
        
        checkCloudAuditEvententity(auditEventEntity, cloudEvent);       
    }
    
    private CloudBPMNTimerFiredEventImpl createTimerFiredEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();
            
        BPMNTimerImpl timer = createBPMNTimer(processInstance);

        CloudBPMNTimerFiredEventImpl event = new CloudBPMNTimerFiredEventImpl("eventId",
                                                                              System.currentTimeMillis(),
                                                                              timer,
                                                                              timer.getProcessDefinitionId(),
                                                                              timer.getProcessInstanceId());
        appendEventInfo(event, processInstance);
        return event;
    }
    
    private CloudBPMNTimerScheduledEventImpl createTimerScheduledEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();
            
        BPMNTimerImpl timer = createBPMNTimer(processInstance);

        CloudBPMNTimerScheduledEventImpl event = new CloudBPMNTimerScheduledEventImpl("eventId",
                                                                                      System.currentTimeMillis(),
                                                                                      timer,
                                                                                      timer.getProcessDefinitionId(),
                                                                                      timer.getProcessInstanceId());
        appendEventInfo(event, processInstance);
        return event;
    }
    
    private CloudBPMNTimerCancelledEventImpl createTimerCancelledEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();
            
        BPMNTimerImpl timer = createBPMNTimer(processInstance);

        CloudBPMNTimerCancelledEventImpl event = new CloudBPMNTimerCancelledEventImpl("eventId",
                                                                                      System.currentTimeMillis(),
                                                                                      timer,
                                                                                      timer.getProcessDefinitionId(),
                                                                                      timer.getProcessInstanceId());
        appendEventInfo(event, processInstance);
        return event;
    }
    
    private CloudBPMNTimerExecutedEventImpl createTimerExecutedEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();
            
        BPMNTimerImpl timer = createBPMNTimer(processInstance);

        CloudBPMNTimerExecutedEventImpl event = new CloudBPMNTimerExecutedEventImpl("eventId",
                                                                                    System.currentTimeMillis(),
                                                                                    timer,
                                                                                    timer.getProcessDefinitionId(),
                                                                                    timer.getProcessInstanceId());
        appendEventInfo(event, processInstance);
        return event;
    }
    
    private CloudBPMNTimerFailedEventImpl createTimerFailedEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();
            
        BPMNTimerImpl timer = createBPMNTimer(processInstance);

        CloudBPMNTimerFailedEventImpl event = new CloudBPMNTimerFailedEventImpl("eventId",
                                                                                System.currentTimeMillis(),
                                                                                timer,
                                                                                timer.getProcessDefinitionId(),
                                                                                timer.getProcessInstanceId());
        appendEventInfo(event, processInstance);
        return event;
    }
    
    private CloudBPMNTimerRetriesDecrementedEventImpl createTimerRetriesDecrementedEvent() {
        //given
        ProcessInstanceImpl processInstance = createProcess();
            
        BPMNTimerImpl timer = createBPMNTimer(processInstance);

        CloudBPMNTimerRetriesDecrementedEventImpl event = new CloudBPMNTimerRetriesDecrementedEventImpl("eventId",
                                                                                                        System.currentTimeMillis(),
                                                                                                        timer,
                                                                                                        timer.getProcessDefinitionId(),
                                                                                                        timer.getProcessInstanceId());
        appendEventInfo(event, processInstance);
        return event;
    }
    
    
    private ProcessInstanceImpl createProcess(){
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("processInstanceId");
        processInstance.setProcessDefinitionId("processDefinitionId");
        processInstance.setProcessDefinitionKey("processDefinitionKey");
        processInstance.setBusinessKey("businessKey");
        processInstance.setParentId("parentId");
        
        return processInstance;
    }
    
    private BPMNTimerImpl createBPMNTimer(ProcessInstanceImpl processInstance) {
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        timer.setProcessInstanceId(processInstance.getId());
        timer.setTimerPayload(createTimerPayload());
        return timer;
    }
    
    private TimerPayload createTimerPayload() {
        TimerPayload timerPayload = new TimerPayload();
        timerPayload.setRetries(5);
        timerPayload.setMaxIterations(2);
        timerPayload.setRepeat("repeat");
        timerPayload.setExceptionMessage("Any message");
        
        return timerPayload;     
    }
    
    private void appendEventInfo(CloudBPMNTimerEventImpl event, ProcessInstance processInstance) {
        event.setEntityId("entityId");
        event.setProcessInstanceId(processInstance.getId());
        event.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        event.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        event.setBusinessKey(processInstance.getBusinessKey());
        event.setParentProcessInstanceId(processInstance.getParentId());
        event.setMessageId("message-id");
        event.setSequenceNumber(0);
        
    }
    
    private void checkCloudAuditEvententity(TimerAuditEventEntity auditEventEntity, CloudBPMNTimerEvent event) {   
        assertThat(event).isNotNull();
        assertThat(auditEventEntity).isNotNull();
        assertThat(auditEventEntity.getEntityId()).isEqualTo(event.getEntityId());
        assertThat(auditEventEntity.getProcessInstanceId()).isEqualTo(event.getProcessInstanceId());
        assertThat(auditEventEntity.getProcessDefinitionId()).isEqualTo(event.getProcessDefinitionId());
        assertThat(auditEventEntity.getProcessDefinitionKey()).isEqualTo(event.getProcessDefinitionKey());
        assertThat(auditEventEntity.getBusinessKey()).isEqualTo(event.getBusinessKey());
        assertThat(auditEventEntity.getParentProcessInstanceId()).isEqualTo(event.getParentProcessInstanceId());
        assertThat(auditEventEntity.getTimer().getProcessInstanceId()).isEqualTo(event.getEntity().getProcessInstanceId());
        assertThat(auditEventEntity.getTimer().getProcessDefinitionId()).isEqualTo(event.getEntity().getProcessDefinitionId());
        assertThat(auditEventEntity.getTimer().getTimerPayload()).isEqualTo(event.getEntity().getTimerPayload());
    }
}