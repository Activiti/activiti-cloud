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
package org.activiti.cloud.modeling.api;

/**
 * Business logic related with actions to be done when the Model metadata changes
 */
public interface ModelUpdateListener {
    /**
     * Perform an extra action over the model from the data received.
     *
     * @param modelToBeUpdated the model as it is before the update
     * @param newModel         the model containing the changes to be performed
     */
    void execute(Model modelToBeUpdated, Model newModel);

    /**
     * Get handled model type by this listener.
     *
     * @return handled model type
     */
    ModelType getHandledModelType();
}
