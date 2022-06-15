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
package org.activiti.cloud.modeling.api.process;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Task Variable Mapping representation
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class TaskVariableMapping {

    private MappingType mappingType;
    private Map<String, ProcessVariableMapping> inputs = new HashMap<>();
    private Map<String, ProcessVariableMapping> outputs = new HashMap<>();

    public Map<String, ProcessVariableMapping> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, ProcessVariableMapping> inputs) {
        this.inputs = inputs;
    }

    public ProcessVariableMapping getInputMapping(String inputName) {
        return inputs.get(inputName);
    }

    public Map<String, ProcessVariableMapping> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, ProcessVariableMapping> outputs) {
        this.outputs = outputs;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(MappingType mappingType) {
        this.mappingType = mappingType;
    }

    public Map<String, ProcessVariableMapping> get(ServiceTaskActionType serviceTaskActionType) {
        if (serviceTaskActionType == ServiceTaskActionType.INPUTS) {
            return getInputs();
        } else if (serviceTaskActionType == ServiceTaskActionType.OUTPUTS) {
            return getOutputs();
        } else {
            return new HashMap<>();
        }
    }

    public enum MappingType {
        MAP_ALL,
        MAP_ALL_INPUTS,
        MAP_ALL_OUTPUTS
    }
}
