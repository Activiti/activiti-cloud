package org.activiti.cloud.services.organization.converter;

import java.util.HashMap;
import java.util.Map;

import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ReferenceIdOverriderTest {

    private ReferenceIdOverrider referenceIdOverrider;

    @Before
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
    public void should_overrideCallActivity_when_hasNewCalledElement() {
        CallActivity callActivity = new CallActivity();
        callActivity.setCalledElement("oldCalledElement");

        referenceIdOverrider.override(callActivity);

        assertThat(callActivity.getCalledElement()).isEqualTo("newCalledElement");
    }

    @Test
    public void should_notOverrideCallActivity_when_sameCalledElement() {
        CallActivity callActivity = new CallActivity();
        callActivity.setCalledElement("sameCalledElement");

        referenceIdOverrider.override(callActivity);

        assertThat(callActivity.getCalledElement()).isEqualTo("sameCalledElement");
    }

    @Test
    public void should_overrideCallActivity_when_notNewCalledElement() {
        CallActivity callActivity = new CallActivity();
        callActivity.setCalledElement("notNewCalledElement");

        referenceIdOverrider.override(callActivity);

        assertThat(callActivity.getCalledElement()).isEqualTo("notNewCalledElement");
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

    @Test
    public void should_overrideProcessId_when_newProcessId() {
        Process process = new Process();
        process.setId("oldProcessId");

        referenceIdOverrider.overrideProcessId(process);

        assertThat(process.getId()).isEqualTo("newProcessId");
    }

    @Test
    public void should_notOverrideProcessId_when_processIdNotFound() {
        Process process = new Process();
        process.setId("notNewProcessId");

        referenceIdOverrider.overrideProcessId(process);

        assertThat(process.getId()).isEqualTo("notNewProcessId");
    }
}