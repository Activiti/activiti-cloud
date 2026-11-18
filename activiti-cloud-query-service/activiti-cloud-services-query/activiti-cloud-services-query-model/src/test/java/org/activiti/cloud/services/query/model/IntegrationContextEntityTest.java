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
package org.activiti.cloud.services.query.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntegrationContextEntityTest {

    private static final String SERVICE_TASK_TYPE = "Rest Connector: GET";

    @Test
    void should_deriveServiceTaskType_fromLinkedServiceTask() {
        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity();
        serviceTaskEntity.setServiceTaskType(SERVICE_TASK_TYPE);
        IntegrationContextEntity entity = new IntegrationContextEntity();
        entity.setServiceTask(serviceTaskEntity);

        assertThat(entity.getServiceTaskType()).isEqualTo(SERVICE_TASK_TYPE);
    }

    @Test
    void should_returnNullServiceTaskType_when_noLinkedServiceTask() {
        IntegrationContextEntity entity = new IntegrationContextEntity();

        assertThat(entity.getServiceTaskType()).isNull();
    }
}
