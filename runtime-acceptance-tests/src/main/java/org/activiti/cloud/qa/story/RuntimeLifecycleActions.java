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

package org.activiti.cloud.qa.story;

import net.thucydides.core.annotations.Steps;
import org.activiti.cloud.qa.rest.DirtyContextHandler;
import org.activiti.cloud.qa.rest.EnableDirtyContext;
import org.activiti.cloud.qa.steps.AuthenticationSteps;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.BeforeStories;
import org.springframework.beans.factory.annotation.Autowired;
import steps.audit.AuditSteps;
import steps.audit.admin.AuditAdminSteps;
import steps.query.ProcessQuerySteps;
import steps.query.TaskQuerySteps;
import steps.query.admin.ProcessQueryAdminSteps;
import steps.runtime.ProcessRuntimeBundleSteps;
import steps.runtime.TaskRuntimeBundleSteps;
import steps.runtime.admin.ProcessRuntimeAdminSteps;

/**
 * Lifecycle steps
 */
@EnableDirtyContext
public class RuntimeLifecycleActions {

    @Autowired
    private DirtyContextHandler dirtyContextHandler;

    @Steps
    private AuthenticationSteps authenticationSteps;

    @Steps
    private ProcessRuntimeBundleSteps processRuntimeBundleSteps;
    @Steps
    private TaskRuntimeBundleSteps taskRuntimeBundleSteps;
    @Steps
    private ProcessRuntimeAdminSteps processRuntimeAdminSteps;

    @Steps
    private ProcessQuerySteps processQuerySteps;
    @Steps
    private TaskQuerySteps taskQuerySteps;
    @Steps
    private ProcessQueryAdminSteps processQueryAdminSteps;

    @Steps
    private AuditSteps auditSteps;
    @Steps
    private AuditAdminSteps auditAdminSteps;

    @BeforeStories
    public void checkServicesHealth() throws Exception {
        authenticationSteps.authenticateUser("testuser");
        processRuntimeBundleSteps.checkServicesHealth();
        taskRuntimeBundleSteps.checkServicesHealth();
        processRuntimeAdminSteps.checkServicesHealth();
        processQuerySteps.checkServicesHealth();
        taskQuerySteps.checkServicesHealth();
        processQueryAdminSteps.checkServicesHealth();
        auditSteps.checkServicesHealth();
        auditAdminSteps.checkServicesHealth();
    }

    @AfterScenario
    public void cleanup() {
        dirtyContextHandler.cleanup();
    }

}
