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
package org.activiti.cloud.services.modeling.jpa;

import java.util.List;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link ModelEntity} entity
 */
@RepositoryRestResource(path = "models", collectionResourceRel = "models", itemResourceRel = "models", exported = false)
public interface ModelJpaRepository extends JpaRepository<ModelEntity, String> {
    @Query("SELECT m FROM Model m LEFT JOIN m.projects p WHERE p.id=:projectId AND m.type=:modelTypeFilter")
    Page<ModelEntity> findAllByProjectIdAndTypeEquals(
        @Param("projectId") String projectId,
        @Param("modelTypeFilter") String modelTypeFilter,
        Pageable pageable
    );

    @Query(
        "SELECT m FROM Model m LEFT JOIN m.projects p WHERE p.id=:projectId AND m.displayName=:modelName AND m.type=:modelTypeFilter"
    )
    List<ModelEntity> findModelByProjectIdAndNameEqualsAndTypeEquals(
        @Param("projectId") String projectId,
        @Param("modelName") String modelName,
        @Param("modelTypeFilter") String modelTypeFilter
    );

    @Query(
            "SELECT m FROM Model m LEFT JOIN m.projects p WHERE p.id=:projectId AND m.key=:modelKey AND m.type=:modelTypeFilter"
    )
    List<ModelEntity> findModelByProjectIdAndKeyEqualsAndTypeEquals(
            @Param("projectId") String projectId,
            @Param("modelKey") String modelKey,
            @Param("modelTypeFilter") String modelTypeFilter
    );

    @Query("SELECT m FROM Model m WHERE m.displayName=:modelName AND m.scope=:scope AND m.type=:modelTypeFilter")
    List<ModelEntity> findModelByNameAndScopeAndTypeEquals(
        @Param("modelName") String modelName,
        @Param("scope") ModelScope scope,
        @Param("modelTypeFilter") String modelTypeFilter
    );

    Page<ModelEntity> findAllByScopeAndTypeEquals(ModelScope scope, String modelTypeFilter, Pageable pageable);

    @Query(
        "SELECT m FROM Model m LEFT JOIN m.projects p WHERE m.type=:modelTypeFilter AND (m.scope=:scope OR p.id IS NULL)"
    )
    Page<ModelEntity> findAllByScopeAndTypeEqualsWithOrphans(
        @Param("scope") ModelScope scope,
        @Param("modelTypeFilter") String modelTypeFilter,
        Pageable pageable
    );

    @Query(
        "SELECT m FROM Model m LEFT JOIN m.projects p WHERE p.id=:projectId AND lower(m.displayName) LIKE lower(concat('%', :name, '%'))"
    )
    Page<ModelEntity> findAllByProjectIdAndNameLike(
        @Param("projectId") String projectId,
        @Param("name") String name,
        Pageable pageable
    );
}
