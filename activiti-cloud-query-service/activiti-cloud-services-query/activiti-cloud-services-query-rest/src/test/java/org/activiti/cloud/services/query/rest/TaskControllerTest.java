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
package org.activiti.cloud.services.query.rest;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.rest.assembler.TaskRepresentationModelAssembler;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TaskControllerTest {

    private TaskController taskController;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskRepresentationModelAssembler taskRepresentationModelAssembler;

    @Mock
    private EntityFinder entityFinder;

    @Mock
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @Mock
    private SecurityManager securityManager;

    @Mock
    private TaskControllerHelper taskControllerHelper;

    @Mock
    private TaskPermissionsHelper taskPermissionsHelper;

    @BeforeEach
    public void setUp() {
        taskController = new TaskController(
            taskRepository,
            taskRepresentationModelAssembler,
            entityFinder,
            taskLookupRestrictionService,
            securityManager,
            taskControllerHelper,
            taskPermissionsHelper
        );
    }

    @Test
    void shouldHandleInvalidDataAccessApiUsageException() {
        // given
        String errorMessage = "Invalid order by clause";
        InvalidDataAccessApiUsageException exception = new InvalidDataAccessApiUsageException(errorMessage);

        // when
        ResponseEntity<String> responseEntity = taskController.handleInvalidDataAccessApiUsageException(exception);

        // then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isEqualTo("Invalid search parameter: " + errorMessage);
    }
}
