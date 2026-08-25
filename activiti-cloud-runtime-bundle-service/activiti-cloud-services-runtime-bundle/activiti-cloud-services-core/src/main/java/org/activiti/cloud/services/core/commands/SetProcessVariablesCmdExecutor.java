/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.EmptyResult;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.cloud.services.core.validation.VariableValueSizeValidator;

public class SetProcessVariablesCmdExecutor extends AbstractCommandExecutor<SetProcessVariablesPayload> {

    private ProcessAdminRuntime processAdminRuntime;
    private VariableValueSizeValidator variableValueSizeValidator;

    public SetProcessVariablesCmdExecutor(
        ProcessAdminRuntime processAdminRuntime,
        VariableValueSizeValidator variableValueSizeValidator
    ) {
        this.processAdminRuntime = processAdminRuntime;
        this.variableValueSizeValidator = variableValueSizeValidator;
    }

    @Override
    public EmptyResult execute(SetProcessVariablesPayload setProcessVariablesPayload) {
        variableValueSizeValidator.validate(setProcessVariablesPayload);
        processAdminRuntime.setVariables(setProcessVariablesPayload);

        return new EmptyResult(setProcessVariablesPayload);
    }
}
