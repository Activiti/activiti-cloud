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
package org.activiti.cloud.services.core;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.activiti.api.process.model.builders.MessagePayloadBuilder;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.cloud.services.api.model.ProcessVariableValue;
import org.springframework.util.Assert;

public class VariablesPayloadConverter {

    private final VariableValueConverter variableValueConverter;

    public VariablesPayloadConverter(VariableValueConverter variableValueConverter) {
        Assert.notNull(variableValueConverter, "VariableValueConverter must not be null");

        this.variableValueConverter = variableValueConverter;
    }

    public StartProcessPayload convert(StartProcessPayload payload) {
        return Optional
            .ofNullable(payload)
            .map(StartProcessPayload::getVariables)
            .map(variables ->
                ProcessPayloadBuilder
                    .start()
                    .withBusinessKey(payload.getBusinessKey())
                    .withName(payload.getName())
                    .withProcessDefinitionId(payload.getProcessDefinitionId())
                    .withProcessDefinitionKey(payload.getProcessDefinitionKey())
                    .withVariables(mapVariableValues(variables))
                    .build()
            )
            .orElse(payload);
    }

    public CompleteTaskPayload convert(CompleteTaskPayload payload) {
        return Optional
            .ofNullable(payload)
            .map(CompleteTaskPayload::getVariables)
            .map(variables ->
                TaskPayloadBuilder
                    .complete()
                    .withTaskId(payload.getTaskId())
                    .withVariables(mapVariableValues(variables))
                    .build()
            )
            .orElse(payload);
    }

    public SaveTaskPayload convert(SaveTaskPayload payload) {
        return Optional
            .ofNullable(payload)
            .map(SaveTaskPayload::getVariables)
            .map(variables ->
                TaskPayloadBuilder
                    .save()
                    .withTaskId(payload.getTaskId())
                    .withVariables(mapVariableValues(variables))
                    .build()
            )
            .orElse(payload);
    }

    public StartMessagePayload convert(StartMessagePayload payload) {
        return Optional
            .ofNullable(payload)
            .map(StartMessagePayload::getVariables)
            .map(variables -> MessagePayloadBuilder.from(payload).withVariables(mapVariableValues(variables)).build())
            .orElse(payload);
    }

    private Map<String, Object> mapVariableValues(Map<String, Object> input) {
        return input
            .entrySet()
            .stream()
            .map(this::parseValue)
            .collect(LinkedHashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
    }

    private Map.Entry<String, Object> parseValue(Map.Entry<String, Object> entry) {
        Object entryValue = entry.getValue();

        try {
            if (Map.class.isInstance(entryValue)) {
                Map<String, String> valuesMap = Map.class.cast(entryValue);

                if (valuesMap.containsKey("type") && valuesMap.containsKey("value")) {
                    String type = valuesMap.get("type");
                    String value = valuesMap.get("value");

                    entryValue = variableValueConverter.convert(new ProcessVariableValue(type, value));
                }
            } else if (ProcessVariableValue.class.isInstance(entryValue)) {
                entryValue = variableValueConverter.convert(ProcessVariableValue.class.cast(entryValue));
            }
        } catch (Exception ignored) {}

        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entryValue);
    }
}
