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

package org.activiti.cloud.services.rest.controllers;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ResumeProcess;
import org.activiti.runtime.api.cmd.SuspendProcess;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessInstanceControllerImplTest {

    @InjectMocks
    private ProcessInstanceControllerImpl controller;

    @Mock
    private SecurityAwareProcessInstanceService processInstanceService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void suspendShouldCallSuspendOnRuntimeService() {
        //given
        String processInstanceId = "7";

        //when
        controller.suspend(processInstanceId);

        //then
        verify(processInstanceService).suspend(any(SuspendProcess.class));
    }

    @Test
    public void activateShouldCallActivateOnRuntimeService() {
        //given
        String processInstanceId = "7";

        //when
        controller.activate(processInstanceId);

        //then
        verify(processInstanceService).activate(any(ResumeProcess.class));
    }
}