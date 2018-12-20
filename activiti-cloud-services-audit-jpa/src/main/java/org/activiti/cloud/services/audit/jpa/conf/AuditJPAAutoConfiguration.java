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

package org.activiti.cloud.services.audit.jpa.conf;

import org.activiti.cloud.services.audit.jpa.converters.ActivityCancelledEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ActivityCompletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ActivityStartedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.EventContextInfoAppender;
import org.activiti.cloud.services.audit.jpa.converters.ProcessCancelledEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessCompletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessResumedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessStartedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessSuspendedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.ProcessUpdatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.SequenceFlowTakenEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskAssignedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCancelledEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCompletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskSuspendedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.TaskUpdatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableCreatedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableDeletedEventConverter;
import org.activiti.cloud.services.audit.jpa.converters.VariableUpdatedEventConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditJPAAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public EventContextInfoAppender eventContextInfoAppender(){
        return new EventContextInfoAppender();
    }

    @ConditionalOnMissingBean
    @Bean
    public ActivityCancelledEventConverter activityCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ActivityCancelledEventConverter(eventContextInfoAppender);
    }

    @ConditionalOnMissingBean
    @Bean
    public ActivityCompletedEventConverter activityCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ActivityCompletedEventConverter(eventContextInfoAppender);
    }
    
    @ConditionalOnMissingBean
    @Bean
    public ActivityStartedEventConverter activityStartedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ActivityStartedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessCancelledEventConverter processCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessCancelledEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessCompletedEventConverter processCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessCompletedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessCreatedEventConverter processCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessCreatedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessResumedEventConverter processResumedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessResumedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessStartedEventConverter processStartedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessStartedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessSuspendedEventConverter processSuspendedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessSuspendedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public ProcessUpdatedEventConverter processUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new ProcessUpdatedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public SequenceFlowTakenEventConverter sequenceFlowTakenEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new SequenceFlowTakenEventConverter(eventContextInfoAppender);
    }    
    
    @ConditionalOnMissingBean
    @Bean
    public TaskAssignedEventConverter taskAssignedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new TaskAssignedEventConverter(eventContextInfoAppender);
    }   
    
    @ConditionalOnMissingBean
    @Bean
    public TaskCancelledEventConverter taskCancelledEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new TaskCancelledEventConverter(eventContextInfoAppender);
    }       
    
    @ConditionalOnMissingBean
    @Bean
    public TaskCompletedEventConverter taskCompletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new TaskCompletedEventConverter(eventContextInfoAppender);
    }       
    
    @ConditionalOnMissingBean
    @Bean
    public TaskCreatedEventConverter taskCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new TaskCreatedEventConverter(eventContextInfoAppender);
    }      
    
    @ConditionalOnMissingBean
    @Bean
    public TaskSuspendedEventConverter taskSuspendedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new TaskSuspendedEventConverter(eventContextInfoAppender);
    }      
    
    @ConditionalOnMissingBean
    @Bean
    public TaskUpdatedEventConverter taskUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new TaskUpdatedEventConverter(eventContextInfoAppender);
    }      
    
    @ConditionalOnMissingBean
    @Bean
    public VariableCreatedEventConverter variableCreatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new VariableCreatedEventConverter(eventContextInfoAppender);
    }      
 
    @ConditionalOnMissingBean
    @Bean
    public VariableDeletedEventConverter variableDeletedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new VariableDeletedEventConverter(eventContextInfoAppender);
    }      

    @ConditionalOnMissingBean
    @Bean
    public VariableUpdatedEventConverter variableUpdatedEventConverter(EventContextInfoAppender eventContextInfoAppender) {
        return new VariableUpdatedEventConverter(eventContextInfoAppender);
    }      
    
}
