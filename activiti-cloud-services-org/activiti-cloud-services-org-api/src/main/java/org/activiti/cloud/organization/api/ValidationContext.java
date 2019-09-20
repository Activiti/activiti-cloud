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

package org.activiti.cloud.organization.api;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Generic context for validations
 */
public interface ValidationContext {

    ValidationContext EMPTY_CONTEXT = new ValidationContext() {
        @Override
        public List<Model> getAvailableModels(ModelType modelType) {
            return emptyList();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    List<Model> getAvailableModels(ModelType modelType);

    boolean isEmpty();
}
