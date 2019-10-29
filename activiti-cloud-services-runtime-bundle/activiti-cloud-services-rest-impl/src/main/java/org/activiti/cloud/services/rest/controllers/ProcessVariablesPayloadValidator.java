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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.common.util.DateFormatterProvider;
import org.activiti.engine.ActivitiException;
import org.activiti.spring.process.ProcessExtensionService;
import org.activiti.spring.process.model.Extension;
import org.activiti.spring.process.model.ProcessExtensionModel;
import org.activiti.spring.process.model.VariableDefinition;
import org.activiti.spring.process.variable.VariableValidationService;

public class ProcessVariablesPayloadValidator {

    private final VariableValidationService variableValidationService;
    private final DateFormatterProvider dateFormatterProvider;
    private final ProcessExtensionService processExtensionService;

    public ProcessVariablesPayloadValidator(DateFormatterProvider dateFormatterProvider,
                                            ProcessExtensionService processExtensionService,
                                            VariableValidationService variableValidationService) {
        this.dateFormatterProvider = dateFormatterProvider;
        this.processExtensionService = processExtensionService;
        this.variableValidationService = variableValidationService;
    }

    private Optional<Map<String, VariableDefinition>> getVariableDefinitionMap(String processDefinitionId) {
        ProcessExtensionModel processExtensionModel = processExtensionService.getExtensionsForId(processDefinitionId);

        return Optional.ofNullable(processExtensionModel)
                .map(ProcessExtensionModel::getExtensions)
                .map(Extension::getProperties);
    }

    private void checkPayloadVariables(Map<String, Object> variablePayloadMap,
                                       String processDefinitionId) {

        final String errorDateTimeParse = "Error parsing date/time variable with a name {0}: {1}";

        final Optional<Map<String, VariableDefinition>> variableDefinitionMap = getVariableDefinitionMap(processDefinitionId);
        List<ActivitiException> activitiExceptions = new ArrayList<>();

        if (variableDefinitionMap.isPresent()) {

            for (Map.Entry<String, Object> payloadVar : variablePayloadMap.entrySet()) {
                String name = payloadVar.getKey();
                Object value = payloadVar.getValue();
                boolean found = false;
                for (Map.Entry<String, VariableDefinition> variableDefinitionEntry : variableDefinitionMap.get().entrySet()) {

                    if (variableDefinitionEntry.getValue().getName().equals(name)) {
                        String type = variableDefinitionEntry.getValue().getType();
                        found = true;

                        if ("date".equals(type) && value != null) {
                            try {
                                payloadVar.setValue(dateFormatterProvider.toDate(value));
                            } catch (Exception e) {
                                activitiExceptions.add(new ActivitiException(MessageFormat.format(errorDateTimeParse, name, e.getMessage())));
                            }
                        } else {
                            activitiExceptions.addAll(variableValidationService.validateWithErrors(value, variableDefinitionEntry.getValue()));
                        }

                        break;
                    }
                }

                if (!found) {
                    //Try to parse a new string variable as date
                    if (value != null && (value instanceof String)) {
                        try {
                            payloadVar.setValue(dateFormatterProvider.toDate(value));
                        } catch (Exception e) {
                            //Do nothing here, keep value as a string
                        }
                    }
                }


            }
        }

        if (!activitiExceptions.isEmpty()) {
            throw new IllegalStateException(activitiExceptions.stream()
                    .map(ex -> ex.getMessage())
                    .collect(Collectors.joining(",")));
        }
    }

    public void checkPayloadVariables(SetProcessVariablesPayload setProcessVariablesPayload,
                                      String processDefinitionId) {

        checkPayloadVariables(setProcessVariablesPayload.getVariables(),
                processDefinitionId);
    }


    public void checkStartProcessPayloadVariables(StartProcessPayload startProcessPayload,
                                                  String processDefinitionId) {

        checkPayloadVariables(startProcessPayload.getVariables(),
                processDefinitionId);
    }
}
