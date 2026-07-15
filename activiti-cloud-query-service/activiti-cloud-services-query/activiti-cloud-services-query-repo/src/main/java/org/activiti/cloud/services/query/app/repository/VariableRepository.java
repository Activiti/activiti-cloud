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

import static org.activiti.cloud.services.query.app.repository.QuerydslBindingsHelper.whitelist;

import com.querydsl.core.types.dsl.StringPath;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface VariableRepository
    extends
        PagingAndSortingRepository<ProcessVariableEntity, Long>,
        JpaSpecificationExecutor<ProcessVariableEntity>,
        QuerydslPredicateExecutor<ProcessVariableEntity>,
        QuerydslBinderCustomizer<QProcessVariableEntity>,
        CrudRepository<ProcessVariableEntity, Long>
{
    @Override
    default void customize(QuerydslBindings bindings, QProcessVariableEntity root) {
        whitelist(root).apply(bindings);

        bindings.bind(String.class).first((StringPath path, String value) -> path.eq(value));
    }

    @Query(
        value = """
            SELECT id, name, type, process_instance_id AS processInstanceId,
                   process_definition_key AS processDefinitionKey, value_size AS valueSize
            FROM (
                SELECT pv.id, pv.name, pv.type, pv.process_instance_id, pv.process_definition_key,
                       length(cast("value" as text)) AS value_size
                FROM process_variable pv
            ) pv
            WHERE value_size >= :minSize
            ORDER BY value_size DESC
            """,
        countQuery = """
            SELECT count(*) FROM process_variable pv
            WHERE length(cast("value" as text)) >= :minSize
            """,
        nativeQuery = true
    )
    Page<Object[]> findLargeVariables(@Param("minSize") int minSize, Pageable pageable);
}
