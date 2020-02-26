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

package org.activiti.cloud.services.core.conf;

import java.util.Set;

import org.activiti.api.model.shared.Payload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.activiti.cloud.services.core.ProcessDiagramGeneratorWrapper;
import org.activiti.cloud.services.core.commands.ClaimTaskCmdExecutor;
import org.activiti.cloud.services.core.commands.CommandEndpoint;
import org.activiti.cloud.services.core.commands.CommandExecutor;
import org.activiti.cloud.services.core.commands.CompleteTaskCmdExecutor;
import org.activiti.cloud.services.core.commands.CreateTaskVariableCmdExecutor;
import org.activiti.cloud.services.core.commands.DeleteProcessInstanceCmdExecutor;
import org.activiti.cloud.services.core.commands.ReceiveMessageCmdExecutor;
import org.activiti.cloud.services.core.commands.ReleaseTaskCmdExecutor;
import org.activiti.cloud.services.core.commands.RemoveProcessVariablesCmdExecutor;
import org.activiti.cloud.services.core.commands.ResumeProcessInstanceCmdExecutor;
import org.activiti.cloud.services.core.commands.SetProcessVariablesCmdExecutor;
import org.activiti.cloud.services.core.commands.SignalCmdExecutor;
import org.activiti.cloud.services.core.commands.StartMessageCmdExecutor;
import org.activiti.cloud.services.core.commands.StartProcessInstanceCmdExecutor;
import org.activiti.cloud.services.core.commands.SuspendProcessInstanceCmdExecutor;
import org.activiti.cloud.services.core.commands.UpdateTaskVariableCmdExecutor;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.core.pageable.sort.ProcessDefinitionSortApplier;
import org.activiti.cloud.services.core.pageable.sort.ProcessInstanceSortApplier;
import org.activiti.cloud.services.core.pageable.sort.TaskSortApplier;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/command-endpoint-channels.properties")
public class ServicesCoreAutoConfiguration {

    @Bean
    public SpringPageConverter pageConverter(){
        return new SpringPageConverter();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ClaimTaskCmdExecutor claimTaskCmdExecutor(TaskAdminRuntime taskAdminRuntime) {
        return new ClaimTaskCmdExecutor(taskAdminRuntime);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CompleteTaskCmdExecutor completeTaskCmdExecutor(TaskAdminRuntime taskAdminRuntime) {
        return new CompleteTaskCmdExecutor(taskAdminRuntime);
    }

    @Bean
    @ConditionalOnMissingBean
    public CreateTaskVariableCmdExecutor createTaskVariableCmdExecutor(TaskAdminRuntime taskAdminRuntime) {
        return new CreateTaskVariableCmdExecutor(taskAdminRuntime);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ReleaseTaskCmdExecutor releaseTaskCmdExecutor(TaskAdminRuntime taskAdminRuntime) {
        return new ReleaseTaskCmdExecutor(taskAdminRuntime);
    }    

    @Bean
    @ConditionalOnMissingBean
    public UpdateTaskVariableCmdExecutor updateTaskVariableCmdExecutor(TaskAdminRuntime taskAdminRuntime) {
        return new UpdateTaskVariableCmdExecutor(taskAdminRuntime);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RemoveProcessVariablesCmdExecutor removeProcessVariablesCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new RemoveProcessVariablesCmdExecutor(processAdminRuntime);
    }    

    @Bean
    @ConditionalOnMissingBean
    public ResumeProcessInstanceCmdExecutor resumeProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new ResumeProcessInstanceCmdExecutor(processAdminRuntime);
    }  
    
    @Bean
    @ConditionalOnMissingBean
    public SetProcessVariablesCmdExecutor setProcessVariablesCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new SetProcessVariablesCmdExecutor(processAdminRuntime);
    }

    @Bean
    @ConditionalOnMissingBean
    public SignalCmdExecutor signalCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new SignalCmdExecutor(processAdminRuntime);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StartProcessInstanceCmdExecutor startProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new StartProcessInstanceCmdExecutor(processAdminRuntime);
    }

    @Bean
    @ConditionalOnMissingBean
    public SuspendProcessInstanceCmdExecutor suspendProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new SuspendProcessInstanceCmdExecutor(processAdminRuntime);
    }    

    @Bean
    @ConditionalOnMissingBean
    public StartMessageCmdExecutor startMessageCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new StartMessageCmdExecutor(processAdminRuntime);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReceiveMessageCmdExecutor receiveMessageCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new ReceiveMessageCmdExecutor(processAdminRuntime);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DeleteProcessInstanceCmdExecutor deleteProcessInstanceCmdExecutor(ProcessAdminRuntime processAdminRuntime) {
        return new DeleteProcessInstanceCmdExecutor(processAdminRuntime);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public <T extends Payload> CommandEndpoint<T> commandEndpoint(Set<CommandExecutor<T>> cmdExecutors) {
        return new CommandEndpoint<T>(cmdExecutors);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessDefinitionSortApplier processDefinitionSortApplier() {
        return new ProcessDefinitionSortApplier();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceSortApplier processInstanceSortApplier() {
        return new ProcessInstanceSortApplier();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TaskSortApplier taskSortApplier() {
        return new TaskSortApplier();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessDiagramGenerator processDiagramGenerator() {
        return new DefaultProcessDiagramGenerator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessDiagramGeneratorWrapper processDiagramGeneratorWrapper(ProcessDiagramGenerator processDiagramGenerator) {
        return new ProcessDiagramGeneratorWrapper(processDiagramGenerator);
        
    }
    

}
