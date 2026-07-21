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
package org.activiti.cloud.starter.rb.validation;

import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorFactory;
import org.activiti.validation.ProcessValidatorImpl;

/**
 * Registers the {@link InescapableLoopValidator} on the engine's process validator.
 *
 * <p>This configurer is only created as a bean when the feature flag
 * {@code activiti.cloud.validation.inescapable-loop.enabled} is {@code true}, so the
 * rule is opt-in and off by default.
 */
public class InescapableLoopValidationConfigurer implements ProcessEngineConfigurationConfigurer {

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        ProcessValidator processValidator = processEngineConfiguration.getProcessValidator();
        if (processValidator == null) {
            processValidator = new ProcessValidatorFactory().createDefaultProcessValidator();
            processEngineConfiguration.setProcessValidator(processValidator);
        }
        if (
            processValidator instanceof ProcessValidatorImpl validatorImpl && validatorImpl.getValidatorSets() != null
        ) {
            validatorImpl
                .getValidatorSets()
                .forEach(validatorSet -> validatorSet.addValidator(new InescapableLoopValidator()));
        }
    }
}
