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
package org.activiti.cloud.common.messaging.config;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

public class FunctionBindingPropertySource extends PropertySource {

    public static final String SPRING_CLOUD_FUNCTION_DEFINITION = "spring.cloud.function.definition";

    private Set<String> functions = new LinkedHashSet<>();
    private final String definition;

    public FunctionBindingPropertySource(ConfigurableEnvironment environment) {
        super(FunctionBindingPropertySource.class.getSimpleName());

        this.definition = environment.getProperty(SPRING_CLOUD_FUNCTION_DEFINITION, "");

        environment
            .getPropertySources()
            .addAfter(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                this);
    }

    public void register(String name) {
        functions.add(name);
    }

    @Override
    public Object getProperty(String name) {
        if (!SPRING_CLOUD_FUNCTION_DEFINITION.equals(name)) {
            return null;
        }

        return Stream.concat(Stream.of(definition.split(";")).filter(StringUtils::hasText),
                             functions.stream().filter(StringUtils::hasText))
                     .distinct()
                     .collect(Collectors.joining(";"));
    }
}
