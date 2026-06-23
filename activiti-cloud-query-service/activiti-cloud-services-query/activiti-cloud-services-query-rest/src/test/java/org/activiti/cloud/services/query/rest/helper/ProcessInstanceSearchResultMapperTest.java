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
package org.activiti.cloud.services.query.rest.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.cloud.api.process.model.ProcessInstanceSearchResult;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProcessInstanceSearchResultMapperTest {

    @Nested
    class ToResult {

        @Test
        void should_mapAllScalarFields() {
            ProcessInstanceEntity entity = new ProcessInstanceEntity();
            entity.setId("pi-1");
            entity.setName("name");
            entity.setStartDate(new Date(1000));
            entity.setInitiator("user1");
            entity.setBusinessKey("bk");
            entity.setStatus(ProcessInstanceStatus.RUNNING);
            entity.setProcessDefinitionId("pd-id");
            entity.setProcessDefinitionKey("pd-key");
            entity.setProcessDefinitionVersion(2);
            entity.setProcessDefinitionName("pd-name");
            entity.setParentId("parent-1");
            entity.setRootProcessInstanceId("root-1");
            entity.setCompletedDate(new Date(2000));
            entity.setSuspendedDate(new Date(3000));
            entity.setServiceName("svc");
            entity.setServiceFullName("svc-full");
            entity.setServiceVersion("1.0");
            entity.setServiceType("RB");
            entity.setAppName("app");
            entity.setAppVersion("1.0");
            entity.setLinkedProcessInstanceId("linked-1");
            entity.setLinkedProcessInstanceType("task-form");
            entity.setType("call-activity");

            ProcessInstanceSearchResult result = ProcessInstanceSearchResultMapper.toResult(entity, 3L, 5L);

            assertThat(result.getId()).isEqualTo("pi-1");
            assertThat(result.getName()).isEqualTo("name");
            assertThat(result.getStartDate()).isEqualTo(new Date(1000));
            assertThat(result.getInitiator()).isEqualTo("user1");
            assertThat(result.getBusinessKey()).isEqualTo("bk");
            assertThat(result.getStatus()).isEqualTo(ProcessInstanceStatus.RUNNING);
            assertThat(result.getProcessDefinitionId()).isEqualTo("pd-id");
            assertThat(result.getProcessDefinitionKey()).isEqualTo("pd-key");
            assertThat(result.getProcessDefinitionVersion()).isEqualTo(2);
            assertThat(result.getProcessDefinitionName()).isEqualTo("pd-name");
            assertThat(result.getParentId()).isEqualTo("parent-1");
            assertThat(result.getRootProcessInstanceId()).isEqualTo("root-1");
            assertThat(result.getCompletedDate()).isEqualTo(new Date(2000));
            assertThat(result.getServiceName()).isEqualTo("svc");
            assertThat(result.getServiceFullName()).isEqualTo("svc-full");
            assertThat(result.getServiceVersion()).isEqualTo("1.0");
            assertThat(result.getServiceType()).isEqualTo("RB");
            assertThat(result.getAppName()).isEqualTo("app");
            assertThat(result.getAppVersion()).isEqualTo("1.0");
            assertThat(result.getLinkedProcessInstanceId()).isEqualTo("linked-1");
            assertThat(result.getLinkedProcessInstanceType()).isEqualTo("task-form");
            assertThat(result.getSubprocessesCount()).isEqualTo(3L);
            assertThat(result.getLinkedProcessesCount()).isEqualTo(5L);
        }

        @Test
        void should_copyVariablesIntoResult() {
            ProcessInstanceEntity entity = new ProcessInstanceEntity();
            entity.setId("pi-1");

            ProcessVariableEntity var = new ProcessVariableEntity();
            var.setName("var1");
            Set<ProcessVariableEntity> variables = new LinkedHashSet<>();
            variables.add(var);
            entity.setVariables(variables);

            ProcessInstanceSearchResult result = ProcessInstanceSearchResultMapper.toResult(entity, 0L, 0L);

            assertThat(result.getVariables()).hasSize(1).first().extracting("name").isEqualTo("var1");
        }

        @Test
        void should_returnEmptyVariables_whenEntityHasNullVariables() {
            ProcessInstanceEntity entity = new ProcessInstanceEntity();
            entity.setId("pi-1");
            entity.setVariables(null);

            ProcessInstanceSearchResult result = ProcessInstanceSearchResultMapper.toResult(entity, 0L, 0L);

            assertThat(result.getVariables()).isEmpty();
        }

        @Test
        void should_setZeroCounts_whenZeroIsPassed() {
            ProcessInstanceEntity entity = new ProcessInstanceEntity();
            entity.setId("pi-1");

            ProcessInstanceSearchResult result = ProcessInstanceSearchResultMapper.toResult(entity, 0L, 0L);

            assertThat(result.getSubprocessesCount()).isZero();
            assertThat(result.getLinkedProcessesCount()).isZero();
        }
    }
}
