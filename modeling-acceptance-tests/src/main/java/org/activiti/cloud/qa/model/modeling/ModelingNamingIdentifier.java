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
import java.util.List;

import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.Model;

/**
 * Identifier by name
 */
public abstract class ModelingNamingIdentifier<M> implements ModelingIdentifier<M> {

    private List<String> names;

    public ModelingNamingIdentifier(String... names) {
        this.names = Arrays.asList(names);
    }

    public ModelingNamingIdentifier(List<String> names) {
        this.names = names;
    }

    public List<String> getNames() {
        return names;
    }

    @Override
    public boolean test(M modelingContext) {
        return names.contains(getName(modelingContext));
    }

    protected abstract String getName(M modelingContext);

    public static ModelingNamingIdentifier<Project> projectNamed(String name) {
        return new ModelingNamingIdentifier<Project>(name) {
            @Override
            protected String getName(Project project) {
                return project.getName();
            }
        };
    }

    public static ModelingNamingIdentifier projectsNamed(List<String> names) {
        return new ModelingNamingIdentifier<Project>(names) {
            @Override
            protected String getName(Project project) {
                return project.getName();
            }
        };
    }

    public static ModelingNamingIdentifier modelNamed(String name) {
        return new ModelingNamingIdentifier<Model>(name) {
            @Override
            protected String getName(Model project) {
                return project.getName();
            }
        };
    }

}
