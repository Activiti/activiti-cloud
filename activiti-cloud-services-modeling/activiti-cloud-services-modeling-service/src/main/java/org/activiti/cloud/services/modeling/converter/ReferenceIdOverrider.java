package org.activiti.cloud.services.modeling.converter;

import java.util.HashMap;
import java.util.Map;

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
    public void override(StartEvent startEvent) {
        String oldFormKey = startEvent.getFormKey();
        String newFormKey = modelIdentifiers.get(oldFormKey);
        if (newFormKey != null) {
            startEvent.setFormKey(newFormKey);
        }
    }
}
