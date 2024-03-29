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
package org.activiti.cloud.api.events.schema;

import static org.activiti.cloud.api.events.CloudRuntimeEventType.ACTIVITY_STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.junit.jupiter.api.Test;

public class CloudRuntimeEventRegistryTest {

    private CloudRuntimeEventRegistry eventRegistry = new CloudRuntimeEventRegistry();

    @Test
    public void buildRegistry_should_createAMapWithEventTypeNameAsKeyAndEventInterfaceAsValue() {
        assertThat(eventRegistry.buildRegistry())
            .containsEntry(ACTIVITY_STARTED.name(), CloudBPMNActivityStartedEvent.class);
    }
}
