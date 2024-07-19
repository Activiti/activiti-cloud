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
package org.activiti.cloud.services.query.events.handlers;

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManager;
import java.util.Date;
import java.util.Optional;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VariableEntityUpdaterTest {

    @InjectMocks
    private ProcessVariableUpdater updater;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFinder entityManagerFinder;

    @Test
    public void updateShouldUpdateVariableRetrievedByPredicate() {
        //given
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setProcessDefinitionKey("procDefKey");

        ProcessVariableInstance currentVariableEntity = new ProcessVariableInstance();
        currentVariableEntity.setName("var");
        currentVariableEntity.setProcessDefinitionKey("procDefKey");

        processInstanceEntity.getVariables().put(currentVariableEntity.getName(), currentVariableEntity);

        given(entityManagerFinder.findProcessInstanceWithVariables("procInstId"))
            .willReturn(Optional.of(processInstanceEntity));
        Date now = new Date();
        ProcessVariableInstance updatedVariableEntity = new ProcessVariableInstance();
        updatedVariableEntity.setName("var");
        updatedVariableEntity.setType("string");
        updatedVariableEntity.setValue("content");
        updatedVariableEntity.setLastUpdatedTime(now);
        updatedVariableEntity.setProcessInstanceId("procInstId");

        //when
        updater.update(updatedVariableEntity, "error");

        //then
        assertThat(currentVariableEntity).hasType("string").hasValue("content").hasLastUpdatedTime(now);

        verify(entityManager).persist(currentVariableEntity);
    }
}
