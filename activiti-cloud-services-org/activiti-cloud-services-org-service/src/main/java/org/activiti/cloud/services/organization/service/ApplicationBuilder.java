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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.activiti.cloud.services.common.file.FileContent;

/**
 * Builder for applications
 */
public class ApplicationBuilder {

    private String applicationName;

    private List<ModelFile> modelFiles = new ArrayList<>();

    public ApplicationBuilder withApplicationName(String applicationName) {
        if (this.applicationName == null) {
            this.applicationName = applicationName;
        }
        return this;
    }

    public ApplicationBuilder withModelFile(String modelType,
                                            FileContent fileContent) {
        modelFiles.add(new ModelFile(modelType,
                                     fileContent));
        return this;
    }

    public Optional<String> getApplicationName() {
        return Optional.ofNullable(applicationName);
    }

    public List<ModelFile> getModelFiles() {
        return modelFiles;
    }

    class ModelFile {

        private final String modelType;

        private final FileContent fileContent;

        public ModelFile(String modelType,
                         FileContent fileContent) {
            this.modelType = modelType;
            this.fileContent = fileContent;
        }

        public String getModelType() {
            return modelType;
        }

        public FileContent getFileContent() {
            return fileContent;
        }
    }
}
