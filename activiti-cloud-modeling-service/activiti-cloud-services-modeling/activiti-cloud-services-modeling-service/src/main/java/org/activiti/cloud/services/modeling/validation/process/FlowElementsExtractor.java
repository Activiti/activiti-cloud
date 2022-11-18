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

package org.activiti.cloud.services.modeling.validation.process;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowElementsContainer;

public class FlowElementsExtractor {

    public <T extends FlowElement> Set<T> extractFlowElements(BpmnModel bpmnModel, Class<T> flowElementType) {
        final Set<T> flowElements = new HashSet<>();
        bpmnModel.getProcesses().forEach(process -> flowElements.addAll(extractFlowElements(process, flowElementType)));
        return flowElements;
    }

    private <T extends FlowElement> Set<T> extractFlowElements(
        FlowElementsContainer container,
        Class<T> flowElementType
    ) {
        Set<T> flowElements = container
            .getFlowElements()
            .stream()
            .filter(flowElement -> flowElementType.isAssignableFrom(flowElement.getClass()))
            .map(flowElementType::cast)
            .collect(Collectors.toSet());
        container
            .getFlowElements()
            .stream()
            .filter(flowElement -> FlowElementsContainer.class.isAssignableFrom(flowElement.getClass()))
            .map(FlowElementsContainer.class::cast)
            .forEach(childContainer -> flowElements.addAll(extractFlowElements(childContainer, flowElementType)));
        return flowElements;
    }
}
