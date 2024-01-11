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
import java.util.Optional;
import java.util.Set;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.services.modeling.service.ProjectHolder;
import org.apache.commons.lang3.StringUtils;

public abstract class ModelExtensionsImportDecorator {

    protected JsonConverter<Map> jsonMetadataConverter;
    protected Set<ModelType> handledModelTypes;

    public ModelExtensionsImportDecorator(JsonConverter<Map> jsonMetadataConverter, Set<ModelType> handledModelTypes) {
        this.jsonMetadataConverter = jsonMetadataConverter;
        this.handledModelTypes = handledModelTypes;
    }

    public Set<ModelType> getHandledModelTypes() {
        return handledModelTypes;
    }

    public void decorate(Model model, ProjectHolder projectHolder) {
        getExtensions(model, projectHolder).ifPresent(extensions -> decorate(model, extensions));
    }

    protected Optional<Map<String, Object>> getExtensions(Model model, ProjectHolder projectHolder) {
        return projectHolder
            .getModelExtension(model)
            .flatMap(fileMetadata ->
                jsonMetadataConverter
                    .tryConvertToEntity(fileMetadata.getFileContent())
                    .map(extensions -> (Map<String, Object>) extensions)
            );
    }

    protected void decorate(Model model, Map<String, Object> extensions) {
        Map<String, Object> extensionsValueMap = getExtensionsValueMapFromJson(extensions);
        model.setExtensions(extensionsValueMap);
        getNotBlankValue(extensionsValueMap, "name").ifPresent(model::setName);
        getNotBlankValue(extensionsValueMap, "key").ifPresent(model::setKey);
    }

    protected Optional<String> getNotBlankValue(Map<String, Object> extensionsValueMap, String key) {
        return Optional
            .ofNullable(extensionsValueMap)
            .map(map -> (String) map.get(key))
            .filter(StringUtils::isNotBlank);
    }

    protected Map<String, Object> getExtensionsValueMapFromJson(Map<String, Object> extensions) {
        return ((Map<String, Object>) extensions.get("extensions"));
    }
}
