package org.activiti.cloud.services.modeling.converter;

import java.util.HashMap;
import java.util.Map;

import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.ReferenceOverrider;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;

public class ReferenceIdOverrider implements ReferenceOverrider {

    private Map<String, String> modelIdentifiers = new HashMap<>();

    public ReferenceIdOverrider(Map<String, String> modelIdentifiers) {
        this.modelIdentifiers.putAll(modelIdentifiers);
    }

    @Override
    public void override(UserTask userTask) {
        String oldFormKey = userTask.getFormKey();
        String newFormKey = modelIdentifiers.get(oldFormKey);
        if (newFormKey != null) {
            userTask.setFormKey(newFormKey);
        }
    }

    @Override
    public void override(CallActivity callActivity) {
        String oldCalledElement = callActivity.getCalledElement();
        String newCalledElement = modelIdentifiers.get(oldCalledElement);
        if (newCalledElement != null) {
            callActivity.setCalledElement(newCalledElement);
        }
    }

    @Override
    public void override(StartEvent startEvent) {
        String oldFormKey = startEvent.getFormKey();
        String newFormKey = modelIdentifiers.get(oldFormKey);
        if (newFormKey != null) {
            startEvent.setFormKey(newFormKey);
        }
    }

    public void overrideProcessId(Process process) {
        String oldProcessId = process.getId();
        String newProcessId = modelIdentifiers.get(oldProcessId);
        if (newProcessId != null) {
            process.setId(newProcessId);
        }
    }
}
