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

package org.activiti.cloud.services.query.events.handlers;

import java.util.UUID;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.VariableEntity;
import org.activiti.runtime.api.event.CloudVariableDeleted;
import org.activiti.runtime.api.event.impl.CloudVariableDeletedEventImpl;
import org.activiti.runtime.api.model.impl.VariableInstanceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessVariableEntityDeletedHandlerTest {

    @InjectMocks
    private ProcessVariableDeletedEventHandler handler;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private EntityFinder entityFinder;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void handleRemoveVariableFromProcessAndSoftDeleteIt() {
        //given
        CloudVariableDeleted event = buildVariableDeletedEvent();

        VariableEntity variableEntity = new VariableEntity();
        given(entityFinder.findOne(eq(variableRepository), any(Predicate.class), anyString())).willReturn(variableEntity);

        //when
        handler.handle(event);

        //then
        verify(variableRepository).save(variableEntity);
        assertThat(variableEntity.getMarkedAsDeleted()).isTrue();
    }

    private CloudVariableDeleted buildVariableDeletedEvent() {
        return new CloudVariableDeletedEventImpl(new VariableInstanceImpl<>("var", "string", "test",
                                                                            UUID.randomUUID().toString()));
    }

}