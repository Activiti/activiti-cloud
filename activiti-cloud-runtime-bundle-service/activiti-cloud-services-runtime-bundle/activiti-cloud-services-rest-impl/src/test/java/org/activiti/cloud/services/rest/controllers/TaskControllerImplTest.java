/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.rest.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.runtime.TaskIdentificationStrategy;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.core.ProcessVariablesPayloadConverter;
import org.activiti.cloud.services.core.pageable.SpringPageConverter;
import org.activiti.cloud.services.rest.assemblers.TaskRepresentationModelAssembler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TaskControllerImplTest {

    @InjectMocks
    private TaskControllerImpl taskController;

    @Mock
    private TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    @Mock
    private AlfrescoPagedModelAssembler<Task> pagedCollectionModelAssembler;

    @Mock
    private SpringPageConverter pageConverter;

    @Mock
    private TaskRuntime taskRuntime;

    @Mock
    private ProcessVariablesPayloadConverter payloadConverter;

    @Mock
    private Task task;

    @Mock
    private EntityModel<CloudTask> taskModel;

    @Test
    void nextTaskShouldReturnOkWhenTaskExists() {
        TaskIdentificationStrategy strategy = TaskIdentificationStrategy.values()[0];
        given(taskRuntime.nextTask(strategy)).willReturn(task);
        given(taskRepresentationModelAssembler.toModel(task)).willReturn(taskModel);

        ResponseEntity<EntityModel<CloudTask>> response = taskController.nextTask(strategy);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(taskModel);
    }

    @Test
    void nextTaskShouldReturnNoContentWhenNoTaskExists() {
        given(taskRuntime.nextTask(null)).willReturn(null);

        ResponseEntity<EntityModel<CloudTask>> response = taskController.nextTask(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(taskRepresentationModelAssembler, never()).toModel(task);
    }
}
