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
package org.activiti.cloud.services.auditable;

import java.util.Date;

/**
 * Auditable interface
 */
public interface Auditable<U> {
    U getCreatedBy();

    void setCreatedBy(U createdBy);

    Date getCreationDate();

    void setCreationDate(Date creationDate);

    U getLastModifiedBy();

    void setLastModifiedBy(U lastModifiedBy);

    Date getLastModifiedDate();

    void setLastModifiedDate(Date lastModifiedDate);

    default void copyAuditInfo(Auditable<U> source) {
        if (source.getCreatedBy() != null) {
            setCreatedBy(source.getCreatedBy());
        }
        if (source.getCreationDate() != null) {
            setCreationDate(source.getCreationDate());
        }
        if (source.getLastModifiedBy() != null) {
            setLastModifiedBy(source.getLastModifiedBy());
        }
        if (source.getLastModifiedDate() != null) {
            setLastModifiedDate(source.getLastModifiedDate());
        }
    }
}
