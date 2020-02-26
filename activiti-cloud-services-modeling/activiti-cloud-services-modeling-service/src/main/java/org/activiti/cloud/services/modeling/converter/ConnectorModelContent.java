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

package org.activiti.cloud.services.modeling.converter;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.activiti.cloud.modeling.api.ModelContent;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Implementation for the {@link ModelContent} corresponding to connector model type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ConnectorModelContent implements ModelContent {

    private String id;

    private String name;

    private String template;

    private Map<String, ConnectorModelFeature> actions;

    private Map<String, ConnectorModelFeature> events;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, ConnectorModelFeature> getActions() {
        return actions;
    }

    public void setActions(Map<String, ConnectorModelFeature> actions) {
        this.actions = actions;
    }

    public Map<String, ConnectorModelFeature> getEvents() {
        return events;
    }

    public void setEvents(Map<String, ConnectorModelFeature> events) {
        this.events = events;
    }
}
