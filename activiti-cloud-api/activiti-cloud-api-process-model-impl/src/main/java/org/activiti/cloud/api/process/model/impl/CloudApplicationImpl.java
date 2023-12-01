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
package org.activiti.cloud.api.process.model.impl;

import java.util.Objects;
import org.activiti.cloud.api.process.model.CloudApplication;

public class CloudApplicationImpl implements CloudApplication {

    private String id;
    private String name;
    private String version;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CloudApplicationImpl other = (CloudApplicationImpl) obj;
        return (
            Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(version, other.version)
        );
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("CloudApplicationImpl [id=")
            .append(id)
            .append(", name=")
            .append(name)
            .append(", version=")
            .append(version)
            .append("]");
        return builder.toString();
    }
}
