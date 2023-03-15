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
package org.activiti.cloud.services.modeling.converter;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReferenceIdOverriderTest {

    private ReferenceIdOverrider referenceIdOverrider;

    @BeforeEach
    public void setUp() {
        Map<String, String> map = new HashMap<>();
        map.put("oldFormKey", "newFormKey");
        map.put("sameFormKey", "sameFormKey");
        map.put("oldCalledElement", "newCalledElement");
        map.put("sameCalledElement", "sameCalledElement");
        map.put("oldProcessId", "newProcessId");
        referenceIdOverrider = new ReferenceIdOverrider(map);
    }

    @Test
    public void should_overrideUserTask_when_hasNewFormKey() {
        UserTask userTask = new UserTask();
        userTask.setFormKey("oldFormKey");

        referenceIdOverrider.override(userTask);

        assertThat(userTask.getFormKey()).isEqualTo("newFormKey");
    }

    @Test
    public void should_notOverrideUserTask_when_sameFormKey() {
        UserTask userTask = new UserTask();
        userTask.setFormKey("sameFormKey");

        referenceIdOverrider.override(userTask);

        assertThat(userTask.getFormKey()).isEqualTo("sameFormKey");
    }

    @Test
    public void should_overrideUserTask_when_notNewFormKey() {
        UserTask userTask = new UserTask();
        userTask.setFormKey("notNewFormKey");

        referenceIdOverrider.override(userTask);

        assertThat(userTask.getFormKey()).isEqualTo("notNewFormKey");
    }

    @Test
    public void should_overrideStartEvent_when_hasNewFormKey() {
        StartEvent startEvent = new StartEvent();
        startEvent.setFormKey("oldFormKey");

        referenceIdOverrider.override(startEvent);

        assertThat(startEvent.getFormKey()).isEqualTo("newFormKey");
    }

    @Test
    public void should_notOverrideStartEvent_when_sameFormKey() {
        StartEvent startEvent = new StartEvent();
        startEvent.setFormKey("sameFormKey");

        referenceIdOverrider.override(startEvent);

        assertThat(startEvent.getFormKey()).isEqualTo("sameFormKey");
    }

    @Test
    public void should_overrideStartEvent_when_notNewFormKey() {
        StartEvent startEvent = new StartEvent();
        startEvent.setFormKey("notNewFormKey");

        referenceIdOverrider.override(startEvent);

        assertThat(startEvent.getFormKey()).isEqualTo("notNewFormKey");
    }
}
