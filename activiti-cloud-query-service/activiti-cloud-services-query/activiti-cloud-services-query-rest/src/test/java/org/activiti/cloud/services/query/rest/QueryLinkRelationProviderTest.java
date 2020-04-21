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

package org.activiti.cloud.services.query.rest;

import org.activiti.cloud.services.query.model.ActivitiEntityMetadata;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryLinkRelationProviderTest {

    private QueryLinkRelationProvider relProvider = new QueryLinkRelationProvider();

    @Test
    public void getItemResourceRelForShouldReturnProcessDefinitionWhenIsProcessDefinitionEntity() {
        //when
        String itemResourceRel = relProvider.getItemResourceRelFor(ProcessDefinitionEntity.class);

        //then
        assertThat(itemResourceRel).isEqualTo("processDefinition");
    }

    @Test
    public void getItemResourceRelForShouldReturnProcessInstanceWhenIsProcessInstanceEntity() {
        //when
        String itemResourceRel = relProvider.getItemResourceRelFor(ProcessInstanceEntity.class);

        //then
        assertThat(itemResourceRel).isEqualTo("processInstance");
    }

    @Test
    public void getItemResourceRelForShouldReturnTaskWhenIsTaskEntity() {
        //when
        String itemResourceRel = relProvider.getItemResourceRelFor(TaskEntity.class);

        //then
        assertThat(itemResourceRel).isEqualTo("task");
    }

    @Test
    public void getItemResourceRelForShouldReturnVariableWhenIsProcessVariableEntity() {
        //when
        String itemResourceRel = relProvider.getItemResourceRelFor(ProcessVariableEntity.class);

        //then
        assertThat(itemResourceRel).isEqualTo("variable");
    }

    @Test
    public void getItemResourceRelForShouldReturnVariableWhenIsTaskVariableEntity() {
        //when
        String itemResourceRel = relProvider.getItemResourceRelFor(TaskVariableEntity.class);

        //then
        assertThat(itemResourceRel).isEqualTo("variable");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnProcessDefinitionsWhenIsProcessDefinitionEntity() {
        //when
        String collectionResourceRel = relProvider.getCollectionResourceRelFor(ProcessDefinitionEntity.class);

        //then
        assertThat(collectionResourceRel).isEqualTo("processDefinitions");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnProcessInstancesWhenIsProcessInstanceEntity() {
        //when
        String collectionResourceRel = relProvider.getCollectionResourceRelFor(ProcessInstanceEntity.class);

        //then
        assertThat(collectionResourceRel).isEqualTo("processInstances");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnTasksWhenIsTaskEntity() {
        //when
        String collectionResourceRel = relProvider.getCollectionResourceRelFor(TaskEntity.class);

        //then
        assertThat(collectionResourceRel).isEqualTo("tasks");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnVariablesWhenIsProcessVariableEntity() {
        //when
        String collectionResourceRel = relProvider.getCollectionResourceRelFor(ProcessVariableEntity.class);

        //then
        assertThat(collectionResourceRel).isEqualTo("variables");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnVariablesWhenIsTaskVariableEntity() {
        //when
        String collectionResourceRel = relProvider.getCollectionResourceRelFor(TaskVariableEntity.class);

        //then
        assertThat(collectionResourceRel).isEqualTo("variables");
    }

    @Test
    public void shouldSupportProcessDefinitionEntity() {
        //when
        boolean supports = relProvider.supports(ProcessDefinitionEntity.class);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportProcessInstanceEntity() {
        //when
        boolean supports = relProvider.supports(ProcessInstanceEntity.class);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportTaskEntity() {
        //when
        boolean supports = relProvider.supports(TaskEntity.class);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportProcessVariableEntity() {
        //when
        boolean supports = relProvider.supports(ProcessVariableEntity.class);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportTaskVariableEntity() {
        //when
        boolean supports = relProvider.supports(TaskVariableEntity.class);

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldNotSupportUncoveredClasses() {
        //when
        boolean supports = relProvider.supports(ActivitiEntityMetadata.class);

        //then
        assertThat(supports).isFalse();
    }
}
