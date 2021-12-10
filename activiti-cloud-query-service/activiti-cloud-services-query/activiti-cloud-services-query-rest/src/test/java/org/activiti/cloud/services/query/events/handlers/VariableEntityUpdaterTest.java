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

import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.persistence.EntityManager;
import java.util.Date;

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class VariableEntityUpdaterTest {

    @InjectMocks
    private ProcessVariableUpdater updater;

    @Mock
    private EntityManager entityManager;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void updateShouldUpdateVariableRetrievedByPredicate() {
        //given
        ProcessVariableEntity currentVariableEntity = new ProcessVariableEntity();
        currentVariableEntity.setName("var");

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.getVariables().add(currentVariableEntity);

        given(entityManager.find(ProcessInstanceEntity.class,
                                 "procInstId")).willReturn(processInstanceEntity);

        Date now = new Date();
        ProcessVariableEntity updatedVariableEntity = new ProcessVariableEntity();
        updatedVariableEntity.setName("var");
        updatedVariableEntity.setType("string");
        updatedVariableEntity.setValue("content");
        updatedVariableEntity.setLastUpdatedTime(now);
        updatedVariableEntity.setProcessInstanceId("procInstId");

        //when
        updater.update(updatedVariableEntity,
                       "error");

        //then
        assertThat(currentVariableEntity)
                .hasType("string")
                .hasValue("content")
                .hasLastUpdatedTime(now);

        verify(entityManager).persist(currentVariableEntity);
    }

}
