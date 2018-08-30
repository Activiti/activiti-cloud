/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.activiti.cloud.services.common.file.FileContent;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.getContentTypeByExtension;
import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * Builder for applications
 */
public class ApplicationBuilder {

    private String applicationName;

    private Map<String, FileContent> applicationMap = new LinkedHashMap<>();

    public ApplicationBuilder withApplicationName(String applicationName) {
        if (this.applicationName == null) {
            this.applicationName = applicationName;
        }
        return this;
    }

    public ApplicationBuilder addContent(String filename,
                                         byte[] bytes) {
        getContentTypeByExtension(getExtension(filename))
                .map(contentType -> new FileContent(filename,
                                                    contentType,
                                                    bytes))
                .ifPresent(fileContent -> applicationMap.put(fileContent.getFilename(),
                                                             fileContent));
        return this;
    }

    public Optional<String> getApplicationName() {
        return Optional.ofNullable(applicationName);
    }

    public Map<String, FileContent> getApplicationMap() {
        return applicationMap;
    }
}
