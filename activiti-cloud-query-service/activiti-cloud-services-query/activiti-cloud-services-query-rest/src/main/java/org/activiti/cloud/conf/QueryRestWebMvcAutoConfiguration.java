/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.conf;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.rest.QueryRelProvider;
import org.activiti.cloud.services.query.rest.assembler.ProcessDefinitionResourceAssembler;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceResourceAssembler;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceVariableResourceAssembler;
import org.activiti.cloud.services.query.rest.assembler.TaskResourceAssembler;
import org.activiti.cloud.services.query.rest.assembler.TaskVariableResourceAssembler;
import org.activiti.cloud.services.security.ProcessDefinitionFilter;
import org.activiti.cloud.services.security.ProcessDefinitionKeyBasedRestrictionBuilder;
import org.activiti.cloud.services.security.ProcessDefinitionRestrictionService;
import org.activiti.cloud.services.security.ProcessInstanceFilter;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.cloud.services.security.ProcessInstanceVariableFilter;
import org.activiti.cloud.services.security.ProcessVariableLookupRestrictionService;
import org.activiti.cloud.services.security.ProcessVariableRestrictionService;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.cloud.services.security.TaskVariableLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryRestWebMvcAutoConfiguration  {

    @Bean
    @ConditionalOnMissingBean
    public ProcessDefinitionResourceAssembler processDefinitionResourceAssembler() {
        return new ProcessDefinitionResourceAssembler();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceResourceAssembler processInstanceResourceAssembler() {
        return new ProcessInstanceResourceAssembler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceVariableResourceAssembler processInstanceVariableResourceAssembler() {
        return new ProcessInstanceVariableResourceAssembler();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TaskResourceAssembler taskResourceAssembler() {
        return new TaskResourceAssembler();
    }    
    
    @Bean
    @ConditionalOnMissingBean
    public TaskVariableResourceAssembler taskVariableResourceAssembler() {
        return new TaskVariableResourceAssembler();
    }        
    
    @Bean
    @ConditionalOnMissingBean
    public QueryRelProvider processDefinitionRelProvider() {
        return new QueryRelProvider();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TaskLookupRestrictionService taskLookupRestrictionService(SecurityManager securityManager) {
        return new TaskLookupRestrictionService(securityManager);
    }    

    @Bean
    @ConditionalOnMissingBean
    public ProcessDefinitionKeyBasedRestrictionBuilder serviceNameRestrictionBuilder(SecurityPoliciesManager securityPoliciesManager,
                                                                                     SecurityPoliciesProperties securityPoliciesProperties) {
        return new ProcessDefinitionKeyBasedRestrictionBuilder(securityPoliciesManager,
                                                               securityPoliciesProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceVariableFilter processInstanceVariableFilter() {
        return new ProcessInstanceVariableFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessVariableRestrictionService processVariableRestrictionService(SecurityPoliciesManager securityPoliciesManager,
                                                                               ProcessInstanceVariableFilter processInstanceVariableFilter,
                                                                               ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder) {
        return new ProcessVariableRestrictionService(securityPoliciesManager,
                                                     processInstanceVariableFilter,
                                                     restrictionBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessVariableLookupRestrictionService variableLookupRestrictionService(ProcessVariableRestrictionService restrictionService) {
        return new ProcessVariableLookupRestrictionService(restrictionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskVariableLookupRestrictionService taskVariableLookupRestrictionService(TaskLookupRestrictionService taskLookupRestrictionService) {
        return new TaskVariableLookupRestrictionService(taskLookupRestrictionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceFilter processInstanceFilter() {
        return new ProcessInstanceFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessInstanceRestrictionService processInstanceRestrictionService(SecurityPoliciesManager securityPoliciesManager,
                                                                               ProcessInstanceFilter processInstanceFilter,
                                                                               ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder) {
        return new ProcessInstanceRestrictionService(securityPoliciesManager,
                                                     processInstanceFilter,
                                                     restrictionBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessDefinitionFilter processDefinitionFilter() {
        return new ProcessDefinitionFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessDefinitionRestrictionService processDefinitionRestrictionService(SecurityPoliciesManager securityPoliciesManager,
                                                                                   ProcessDefinitionKeyBasedRestrictionBuilder restrictionBuilder,
                                                                                   ProcessDefinitionFilter processDefinitionFilter) {
        return new ProcessDefinitionRestrictionService(securityPoliciesManager,
                                                       restrictionBuilder,
                                                       processDefinitionFilter);
    }
}
