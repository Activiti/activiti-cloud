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

package org.activiti.cloud.services.modeling.rest.validation;

import org.activiti.cloud.modeling.api.ModelValidationErrorProducer;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Abstract payload validator.
 */
public abstract class GenericPayloadValidator<T> implements Validator,
                                                            ModelValidationErrorProducer {

    protected boolean validateRequiredFields;

    protected Class<T> supportedClass;

    public GenericPayloadValidator(Class<T> supportedClass,
                                   boolean validateRequiredFields) {
        this.supportedClass = supportedClass;
        this.validateRequiredFields = validateRequiredFields;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return supportedClass.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target,
                         Errors errors) {
        validatePayload(supportedClass.cast(target),
                        errors);
    }

    protected abstract void validatePayload(T target,
                                            Errors errors);
}
