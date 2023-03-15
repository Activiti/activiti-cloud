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
package org.activiti.cloud.services.modeling.service;

import static org.activiti.cloud.services.modeling.service.ModelTypeComparators.PRIORITIZED_MODEL_TYPES;

import java.util.Comparator;

public class ModelJsonFileTypeComparator implements Comparator<ProjectHolder.ModelJsonFile> {

    @Override
    public int compare(ProjectHolder.ModelJsonFile o1, ProjectHolder.ModelJsonFile o2) {
        if (
            PRIORITIZED_MODEL_TYPES.contains(o2.getModelType().getName()) &&
            !PRIORITIZED_MODEL_TYPES.contains(o1.getModelType().getName())
        ) {
            return 1;
        } else if (
            PRIORITIZED_MODEL_TYPES.contains(o1.getModelType().getName()) &&
            !PRIORITIZED_MODEL_TYPES.contains(o2.getModelType().getName())
        ) {
            return -1;
        }
        return 0;
    }
}
