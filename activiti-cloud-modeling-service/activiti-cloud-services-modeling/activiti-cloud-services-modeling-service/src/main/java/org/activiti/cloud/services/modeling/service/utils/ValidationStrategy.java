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
package org.activiti.cloud.services.modeling.service.utils;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.activiti.cloud.modeling.core.error.ModelingException;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ModelValidator;
import org.springframework.lang.NonNull;

public interface ValidationStrategy<V extends ModelValidator> {
    List<ModelValidationError> getValidationErrors(
        @NonNull Collection<V> validators,
        @NonNull ValidationCallback<V> callback
    );

    void validate(@NonNull Collection<V> validators, @NonNull ValidationCallback<V> callback) throws ModelingException;

    public static interface ValidationCallback<T extends ModelValidator> extends Consumer<T> {
        @Override
        void accept(T validator) throws ModelingException;
    }
}
