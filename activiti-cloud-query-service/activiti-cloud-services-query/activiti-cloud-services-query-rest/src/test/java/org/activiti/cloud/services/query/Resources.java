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
package org.activiti.cloud.services.query;

/**
 * This class defines the name of the resources used by tests to require locks and avoid
 * concurrency and flaky tests
 */
public class Resources {

    public static final String TASK_REPOSITORY = "TaskRepository";

    public static final String PROCESS_INSTANCE_REPOSITORY = "ProcessInstanceRepository";

    public static final String PROCESS_DEFINITION_REPOSITORY = "ProcessDefinitionRepository";

    public static final String VARIABLE_REPOSITORY = "VariableRepository";

    public static final String TASK_VARIABLE_REPOSITORY = "TaskVariableRepository";

    public static final String TASK_CANDIDATE_USER_REPOSITORY = "TaskCandidateUserRepository";

    public static final String TASK_CANDIDATE_GROUP_REPOSITORY = "TaskCandidateGroupRepository";
}
