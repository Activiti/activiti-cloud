/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import java.time.DateTimeException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.activiti.api.task.model.payloads.CreateTaskVariablePayload;
import org.activiti.api.task.model.payloads.UpdateTaskVariablePayload;
import org.activiti.common.util.DateFormatterProvider;

public class TaskVariablesPayloadDateHandler {
   
    private final DateFormatterProvider dateFormatterProvider;
 
    public TaskVariablesPayloadDateHandler(DateFormatterProvider dateFormatterProvider) {
        this.dateFormatterProvider = dateFormatterProvider;
    }
    
    public Map<String, Object> handleDates(Map<String, Object> variablePayloadMap) {
       if (variablePayloadMap != null) {
           variablePayloadMap.entrySet()
                   .stream()
                   .filter(stringObjectEntry -> stringObjectEntry.getValue() instanceof String)
                   .forEach(stringObjectEntry ->
                                   handleAsDate((String) stringObjectEntry.getValue()).ifPresent(stringObjectEntry::setValue));
       }
       return variablePayloadMap;
    }

    private Optional<Date> handleAsDate(String value) {
        try {
            return Optional.ofNullable(dateFormatterProvider.parse(value));
        } catch (DateTimeException e) {
            // ignore exception and return empty: it's not a date so let's keep initial value
            return Optional.empty();
        }
    }
    
    public CreateTaskVariablePayload handleDate(CreateTaskVariablePayload createTaskVariablePayload) {
        Object value = createTaskVariablePayload.getValue();
        if (value instanceof String) {
            handleAsDate((String)value).ifPresent(createTaskVariablePayload::setValue);
        }
        return createTaskVariablePayload;
    }
    
    public UpdateTaskVariablePayload handleDate(UpdateTaskVariablePayload updateTaskVariablePayload) {
        Object value = updateTaskVariablePayload.getValue();
        
        if (value instanceof String) {
            handleAsDate((String) value).ifPresent(updateTaskVariablePayload::setValue);
        }
        return updateTaskVariablePayload;
    }
}
