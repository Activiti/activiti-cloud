// Generated by delombok at Wed Jan 13 19:00:08 PST 2021
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
package org.activiti.cloud.service.common.batch.domain;

import java.util.HashMap;
import java.util.Map;

public class JobConfig {
    private String name;
    private Map<String, Object> properties = new HashMap<>();
    private boolean asynchronous;

    public JobConfig() {
    }

    public JobConfig(final String name, final Map<String, Object> properties, final boolean asynchronous) {
        this.name = name;
        this.properties = properties;
        this.asynchronous = asynchronous;
    }

    public static class JobConfigBuilder {
        private String name;
        private java.util.ArrayList<String> properties$key;
        private java.util.ArrayList<Object> properties$value;
        private boolean asynchronous;

        JobConfigBuilder() {
        }

        public JobConfigBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public JobConfigBuilder property(final String propertyKey, final Object propertyValue) {
            if (this.properties$key == null) {
                this.properties$key = new java.util.ArrayList<String>();
                this.properties$value = new java.util.ArrayList<Object>();
            }
            this.properties$key.add(propertyKey);
            this.properties$value.add(propertyValue);
            return this;
        }

        public JobConfigBuilder properties(final java.util.Map<? extends String, ? extends Object> properties) {
            if (this.properties$key == null) {
                this.properties$key = new java.util.ArrayList<String>();
                this.properties$value = new java.util.ArrayList<Object>();
            }
            for (final java.util.Map.Entry<? extends String, ? extends Object> $lombokEntry : properties.entrySet()) {
                this.properties$key.add($lombokEntry.getKey());
                this.properties$value.add($lombokEntry.getValue());
            }
            return this;
        }

        public JobConfigBuilder clearProperties() {
            if (this.properties$key != null) {
                this.properties$key.clear();
                this.properties$value.clear();
            }
            return this;
        }

        public JobConfigBuilder asynchronous(final boolean asynchronous) {
            this.asynchronous = asynchronous;
            return this;
        }

        public JobConfig build() {
            java.util.Map<String, Object> properties;
            switch (this.properties$key == null ? 0 : this.properties$key.size()) {
            case 0:
                properties = java.util.Collections.emptyMap();
                break;
            case 1:
                properties = java.util.Collections.singletonMap(this.properties$key.get(0), this.properties$value.get(0));
                break;
            default:
                properties = new java.util.LinkedHashMap<String, Object>(this.properties$key.size() < 1073741824 ? 1 + this.properties$key.size() + (this.properties$key.size() - 3) / 3 : java.lang.Integer.MAX_VALUE);
                for (int $i = 0; $i < this.properties$key.size(); $i++) properties.put(this.properties$key.get($i), (Object) this.properties$value.get($i));
                properties = java.util.Collections.unmodifiableMap(properties);
            }
            return new JobConfig(name, properties, asynchronous);
        }

        @Override
        public java.lang.String toString() {
            return "JobConfig.JobConfigBuilder(name=" + this.name + ", properties$key=" + this.properties$key + ", properties$value=" + this.properties$value + ", asynchronous=" + this.asynchronous + ")";
        }
    }

    public static JobConfigBuilder builder() {
        return new JobConfigBuilder();
    }

    public JobConfigBuilder toBuilder() {
        final JobConfigBuilder builder = new JobConfigBuilder().name(this.name).asynchronous(this.asynchronous);
        if (this.properties != null) builder.properties(this.properties);
        return builder;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public boolean isAsynchronous() {
        return this.asynchronous;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setProperties(final Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setAsynchronous(final boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    @Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof JobConfig)) return false;
        final JobConfig other = (JobConfig) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$properties = this.getProperties();
        final java.lang.Object other$properties = other.getProperties();
        if (this$properties == null ? other$properties != null : !this$properties.equals(other$properties)) return false;
        if (this.isAsynchronous() != other.isAsynchronous()) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JobConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $properties = this.getProperties();
        result = result * PRIME + ($properties == null ? 43 : $properties.hashCode());
        result = result * PRIME + (this.isAsynchronous() ? 79 : 97);
        return result;
    }

    @Override
    public String toString() {
        return "JobConfig(name=" + this.getName() + ", properties=" + this.getProperties() + ", asynchronous=" + this.isAsynchronous() + ")";
    }

}
