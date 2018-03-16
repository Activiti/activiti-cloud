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

package org.activiti.cloud.qa.model.modeling;

import java.util.Arrays;

/**
 * Model for modeling process models, form models, ...
 */
public class Model implements ModelingContext {

    public static final String PROJECT_MODELS_REL = "models";

    private String id;

    private String name;

    private ModelType type;

    private String refId;

    private String modelId;

    private String content;

    private String version;

    public Model() {

    }

    public Model(String id,
                 String name,
                 ModelType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.refId = id;
        this.modelId = id;
        this.content = "test";
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModelType getType() {
        return type;
    }

    public void setType(ModelType type) {
        this.type = type;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getRel() {
        return PROJECT_MODELS_REL;
    }

    public enum ModelType {
        FORM("form"),
        PROCESS_MODEL("process");

        private String text;

        ModelType(String text) {
            this.text = text;
        }

        public static ModelType forText(String text) {
            return Arrays.asList(values())
                    .stream()
                    .filter(type -> type.text.equals(text))
                    .findFirst()
                    .orElse(null);
        }
    }}
