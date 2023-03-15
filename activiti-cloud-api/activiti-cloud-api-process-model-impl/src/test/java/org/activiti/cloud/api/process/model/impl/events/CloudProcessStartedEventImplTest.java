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
package org.activiti.cloud.api.process.model.impl.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.junit.jupiter.api.Test;

public class CloudProcessStartedEventImplTest {

    @Test
    public void shouldSetFlattenInfoBasedOnEntity() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("instId");
        processInstance.setProcessDefinitionId("defId");
        processInstance.setBusinessKey("business");

        //when
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl(processInstance);

        //then
        assertThat(processStartedEvent.getProcessInstanceId()).isEqualTo("instId");
        assertThat(processStartedEvent.getEntityId()).isEqualTo("instId");
        assertThat(processStartedEvent.getProcessDefinitionId()).isEqualTo("defId");
        assertThat(processStartedEvent.getBusinessKey()).isEqualTo("business");
    }
}
