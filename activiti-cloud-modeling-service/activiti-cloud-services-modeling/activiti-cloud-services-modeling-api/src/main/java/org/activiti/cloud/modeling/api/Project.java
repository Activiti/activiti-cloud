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
package org.activiti.cloud.modeling.api;

import org.activiti.cloud.services.auditable.Auditable;

/**
 * Interface for projects
 */
public interface Project<U> extends Auditable<U> {
    String getId();

    void setId(String id);

    String getName();

    void setName(String name);

    default String getTechnicalName() {
        return getName();
    }

    default void setTechnicalName(String technicalName) {
        setName(technicalName);
    }

    default String getDisplayName() {
        return getName();
    }

    default void setDisplayName(String displayName) {
        setName(displayName);
    }

    String getVersion();

    void setVersion(String version);

    String getDescription();

    void setDescription(String description);
}
