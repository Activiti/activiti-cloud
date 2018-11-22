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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryRelProviderTest {

    private QueryRelProvider relProvider = new QueryRelProvider();

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
    public void shouldNotSupportUncoveredClasses() {
        //when
        boolean supports = relProvider.supports(ActivitiEntityMetadata.class);

        //then
        assertThat(supports).isFalse();
    }
}