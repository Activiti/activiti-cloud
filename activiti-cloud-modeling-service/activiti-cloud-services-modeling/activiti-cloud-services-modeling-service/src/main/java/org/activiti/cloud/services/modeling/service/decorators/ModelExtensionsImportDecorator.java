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

package org.activiti.cloud.services.modeling.service.decorators;

import java.util.Map;
import java.util.Set;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;

public interface ModelExtensionsImportDecorator {

    Set<ModelType> getHandledModelTypes();

    void decorate(Model model, Map<String, Object> extensions);

    default Map<String, Object> getExtensionsValueMapFromJson(Map<String, Object> extensions) {
        return ((Map<String, Object>) extensions.get("extensions"));
    }
}
