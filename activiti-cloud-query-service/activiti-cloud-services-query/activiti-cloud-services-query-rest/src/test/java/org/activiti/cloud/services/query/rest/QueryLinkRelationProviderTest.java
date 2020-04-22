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
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryLinkRelationProviderTest {

    private QueryLinkRelationProvider relProvider = new QueryLinkRelationProvider();

    @Test
    public void getItemResourceRelForShouldReturnProcessDefinitionWhenIsProcessDefinitionEntity() {
        //when
        LinkRelation itemResourceRel = relProvider.getItemResourceRelFor(ProcessDefinitionEntity.class);

        //then
        assertThat(itemResourceRel.value()).isEqualTo("processDefinition");
    }

    @Test
    public void getItemResourceRelForShouldReturnProcessInstanceWhenIsProcessInstanceEntity() {
        //when
        LinkRelation itemResourceRel = relProvider.getItemResourceRelFor(ProcessInstanceEntity.class);

        //then
        assertThat(itemResourceRel.value()).isEqualTo("processInstance");
    }

    @Test
    public void getItemResourceRelForShouldReturnTaskWhenIsTaskEntity() {
        //when
        LinkRelation itemResourceRel = relProvider.getItemResourceRelFor(TaskEntity.class);

        //then
        assertThat(itemResourceRel.value()).isEqualTo("task");
    }

    @Test
    public void getItemResourceRelForShouldReturnVariableWhenIsProcessVariableEntity() {
        //when
        LinkRelation itemResourceRel = relProvider.getItemResourceRelFor(ProcessVariableEntity.class);

        //then
        assertThat(itemResourceRel.value()).isEqualTo("variable");
    }

    @Test
    public void getItemResourceRelForShouldReturnVariableWhenIsTaskVariableEntity() {
        //when
        LinkRelation itemResourceRel = relProvider.getItemResourceRelFor(TaskVariableEntity.class);

        //then
        assertThat(itemResourceRel.value()).isEqualTo("variable");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnProcessDefinitionsWhenIsProcessDefinitionEntity() {
        //when
        LinkRelation collectionResourceRel = relProvider.getCollectionResourceRelFor(ProcessDefinitionEntity.class);

        //then
        assertThat(collectionResourceRel.value()).isEqualTo("processDefinitions");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnProcessInstancesWhenIsProcessInstanceEntity() {
        //when
        LinkRelation collectionResourceRel = relProvider.getCollectionResourceRelFor(ProcessInstanceEntity.class);

        //then
        assertThat(collectionResourceRel.value()).isEqualTo("processInstances");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnTasksWhenIsTaskEntity() {
        //when
        LinkRelation collectionResourceRel = relProvider.getCollectionResourceRelFor(TaskEntity.class);

        //then
        assertThat(collectionResourceRel.value()).isEqualTo("tasks");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnVariablesWhenIsProcessVariableEntity() {
        //when
        LinkRelation collectionResourceRel = relProvider.getCollectionResourceRelFor(ProcessVariableEntity.class);

        //then
        assertThat(collectionResourceRel.value()).isEqualTo("variables");
    }

    @Test
    public void getCollectionResourceRelForShouldReturnVariablesWhenIsTaskVariableEntity() {
        //when
        LinkRelation collectionResourceRel = relProvider.getCollectionResourceRelFor(TaskVariableEntity.class);

        //then
        assertThat(collectionResourceRel.value()).isEqualTo("variables");
    }

    @Test
    public void shouldSupportProcessDefinitionEntity() {
        //when
        boolean supports = relProvider.supports(LinkRelationProvider.LookupContext.forType(ProcessDefinitionEntity.class));

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportProcessInstanceEntity() {
        //when
        boolean supports = relProvider.supports(LinkRelationProvider.LookupContext.forType(ProcessInstanceEntity.class));

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportTaskEntity() {
        //when
        boolean supports = relProvider.supports(LinkRelationProvider.LookupContext.forType(TaskEntity.class));

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportProcessVariableEntity() {
        //when
        boolean supports = relProvider.supports(LinkRelationProvider.LookupContext.forType(ProcessVariableEntity.class));

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldSupportTaskVariableEntity() {
        //when
        boolean supports = relProvider.supports(LinkRelationProvider.LookupContext.forType(TaskVariableEntity.class));

        //then
        assertThat(supports).isTrue();
    }

    @Test
    public void shouldNotSupportUncoveredClasses() {
        //when
        boolean supports = relProvider.supports(LinkRelationProvider.LookupContext.forType(ActivitiEntityMetadata.class));

        //then
        assertThat(supports).isFalse();
    }
}
