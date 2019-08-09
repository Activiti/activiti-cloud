/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.organization.converter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.CallActivity;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.Task;
import org.activiti.cloud.organization.api.ModelContent;
import org.activiti.cloud.organization.core.error.ModelingException;

/**
 * Implementation of {@link ModelContent} corresponding to process model type based on a {@link BpmnModel}
 */
public class BpmnProcessModelContent implements ModelContent {

    private final BpmnModel bpmnModel;

    private final Process process;

    public BpmnProcessModelContent(BpmnModel bpmnModel) {
        this.bpmnModel = bpmnModel;
        this.process = bpmnModel
                .getProcesses()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ModelingException("Invalid BPMN model: no process found"));
    }

    public BpmnModel getBpmnModel() {
        return bpmnModel;
    }

    @Override
    public String getId() {
        return process.getId();
    }

    public BpmnProcessModelContent setId(String id) {
        process.setId(id);
        return this;
    }

    @Override
    public String getTemplate() {
        return null;
    }

    public Set<String> findAllTaskIds() {
        return findAllActivityIds(Task.class,
                                  CallActivity.class);
    }

    public Set<String> findAllActivityIds(Class<? extends Activity>... activityTypes) {
        return bpmnModel.getProcesses()
                .stream()
                .flatMap(process -> Arrays.stream(activityTypes)
                        .map(process::findFlowElementsOfType)
                        .flatMap(List::stream)
                )
                .map(Activity::getId)
                .collect(Collectors.toSet());
    }
}
