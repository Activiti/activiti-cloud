/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.model;

import java.io.Serializable;
import java.util.Objects;

public class ProcessInstanceHierarchyId implements Serializable {

    private String ancestorId;
    private String descendantId;

    public ProcessInstanceHierarchyId() {}

    public ProcessInstanceHierarchyId(String ancestorId, String descendantId) {
        this.ancestorId = ancestorId;
        this.descendantId = descendantId;
    }

    public String getAncestorId() {
        return ancestorId;
    }

    public String getDescendantId() {
        return descendantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInstanceHierarchyId that = (ProcessInstanceHierarchyId) o;
        return Objects.equals(ancestorId, that.ancestorId) && Objects.equals(descendantId, that.descendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ancestorId, descendantId);
    }
}
