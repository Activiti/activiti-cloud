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

package org.activiti.services.connectors.channel;

import java.util.Optional;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.bpmn.helper.ErrorPropagation;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

class PropagateCloudBpmnErrorCmd implements Command<Void> {

    private final DelegateExecution execution;
    private final IntegrationError integrationError;

    PropagateCloudBpmnErrorCmd(IntegrationError integrationError, DelegateExecution execution) {
        this.integrationError = integrationError;
        this.execution = execution;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        // Fallback to error message for backward compatibility
        String errorCode = Optional
            .ofNullable(integrationError.getErrorCode())
            .orElse(integrationError.getErrorMessage());

        propagateError(errorCode);

        return null;
    }

    protected void propagateError(String errorCode) {
        // throw business fault so that it can be caught by an Error Intermediate Event or Error Event Sub-Process in the process
        ErrorPropagation.propagateError(errorCode, execution);
    }
}
