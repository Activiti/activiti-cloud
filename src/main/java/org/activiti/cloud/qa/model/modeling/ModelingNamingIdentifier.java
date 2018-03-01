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

/**
 * Identifier by name
 */
public class ModelingNamingIdentifier implements ModelingIdentifier {

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
    public boolean test(ModelingContext modelingContext) {
        return names.contains(modelingContext.getName());
    }

    public static ModelingNamingIdentifier named(String name) {
        return new ModelingNamingIdentifier(name);
    }

    public static ModelingNamingIdentifier named(List<String> name) {
        return new ModelingNamingIdentifier(name);
    }
}
