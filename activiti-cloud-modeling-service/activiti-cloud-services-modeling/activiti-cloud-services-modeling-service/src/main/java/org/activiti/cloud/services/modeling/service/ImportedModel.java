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

package org.activiti.cloud.services.modeling.service;

import java.util.Objects;
import org.activiti.cloud.modeling.api.Model;

public final class ImportedModel {

    private final Model model;
    private final String originalId;
    private final String updatedId;

    public ImportedModel(Model model) {
        this(model, null, null);
    }

    public ImportedModel(Model model, String originalId, String updatedId) {
        this.model = model;
        this.originalId = originalId;
        this.updatedId = updatedId;
    }

    public boolean hasIdentifiersToUpdate() {
        return originalId != null && updatedId != null;
    }

    public Model getModel() {
        return model;
    }

    public String getOriginalId() {
        return originalId;
    }

    public String getUpdatedId() {
        return updatedId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ImportedModel) obj;
        return (
            Objects.equals(this.model, that.model) &&
            Objects.equals(this.originalId, that.originalId) &&
            Objects.equals(this.updatedId, that.updatedId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, originalId, updatedId);
    }

    @Override
    public String toString() {
        return (
            "ImportedModel[" +
            "model=" +
            model +
            ", " +
            "originalId=" +
            originalId +
            ", " +
            "updatedId=" +
            updatedId +
            ']'
        );
    }
}