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
package org.activiti.cloud.services.query.app.repository;

import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomizedProcessInstanceRepository {
    Page<ProcessInstanceEntity> mapSubprocesses(Page<ProcessInstanceEntity> processInstances, Pageable pageable);

    Page<ProcessInstanceEntity> mapSubprocesses(Page<ProcessInstanceEntity> processInstances);

    ProcessInstanceEntity mapSubprocesses(ProcessInstanceEntity processInstance);

    Page<ProcessInstanceEntity> mapAllLinkedProcesses(Page<ProcessInstanceEntity> processInstances);
}
