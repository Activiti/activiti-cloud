/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.notifications.graphql.jpa.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.activiti.cloud.services.notifications.graphql.jpa-query")
public class ActivitiGraphQlJPASchemaProperties {

    private boolean enabled = true;
    private List<Class<?>> entities = new ArrayList<>();
    private AggregateProperties aggregate = new AggregateProperties();
    private FieldsVisibilityProperties fieldsVisibility = new FieldsVisibilityProperties();

    private RestrictedKeysProviderProperties restrictedKeysProvider = new RestrictedKeysProviderProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Class<?>> getEntities() {
        return entities;
    }

    public void setEntities(List<Class<?>> entities) {
        this.entities = entities;
    }

    public AggregateProperties getAggregate() {
        return aggregate;
    }

    public void setAggregate(AggregateProperties aggregate) {
        this.aggregate = aggregate;
    }

    public FieldsVisibilityProperties getFieldsVisibility() {
        return fieldsVisibility;
    }

    public void setFieldsVisibility(FieldsVisibilityProperties fieldsVisibility) {
        this.fieldsVisibility = fieldsVisibility;
    }

    public RestrictedKeysProviderProperties getRestrictedKeysProvider() {
        return restrictedKeysProvider;
    }

    public void setRestrictedKeysProvider(RestrictedKeysProviderProperties restrictedKeysProvider) {
        this.restrictedKeysProvider = restrictedKeysProvider;
    }

    public static class AggregateProperties {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class RestrictedKeysProviderProperties {

        private boolean enabled = true;
        private List<String> unrestrictedRoles = List.of("ACTIVITI_ADMIN", "APPLICATION_MANAGER");
        private String rolePrefix = "ROLE_";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getUnrestrictedRoles() {
            return unrestrictedRoles;
        }

        public void setUnrestrictedRoles(List<String> unrestrictedRoles) {
            this.unrestrictedRoles = unrestrictedRoles;
        }

        public String getRolePrefix() {
            return rolePrefix;
        }

        public void setRolePrefix(String rolePrefix) {
            this.rolePrefix = rolePrefix;
        }
    }

    public static class FieldsVisibilityProperties {

        private boolean enabled = true;
        private Map<String, Set<Pattern>> patterns = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Set<Pattern>> getPatterns() {
            return patterns;
        }

        public void setPatterns(Map<String, Set<Pattern>> patterns) {
            this.patterns = patterns;
        }
    }
}
