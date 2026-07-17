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
package org.activiti.cloud.services.query.app.repository;

import java.util.Collection;
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessInstanceHierarchyRepository
    extends JpaRepository<ProcessInstanceHierarchyEntity, ProcessInstanceHierarchyId>
{
    List<ProcessInstanceHierarchyEntity> findByDescendantId(String descendantId);

    List<ProcessInstanceHierarchyEntity> findByAncestorIdInAndDepthGreaterThan(
        Collection<String> ancestorIds,
        int depth
    );

    @Query(
        "select h.ancestorId as ancestorId, h.relationType as relationType, count(h) as relatedCount " +
            "from ProcessInstanceHierarchy h " +
            "where h.ancestorId in :ancestorIds and h.depth > 0 " +
            "group by h.ancestorId, h.relationType"
    )
    List<RelatedProcessCountProjection> countRelatedByAncestor(@Param("ancestorIds") Collection<String> ancestorIds);

    void deleteByAncestorIdOrDescendantId(String ancestorId, String descendantId);

    interface RelatedProcessCountProjection {
        String getAncestorId();
        String getRelationType();
        long getRelatedCount();
    }
}
