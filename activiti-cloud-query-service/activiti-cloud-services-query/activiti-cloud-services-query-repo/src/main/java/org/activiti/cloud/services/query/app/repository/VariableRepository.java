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
import java.util.List;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QProcessVariableEntity;
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
        value = "SELECT combined.source_table AS sourceTable," +
            " combined.process_definition_key AS processDefinitionKey," +
            " pd.name AS processDefinitionName," +
            " pd.version AS processDefinitionVersion," +
            " combined.name AS variableName," +
            " combined.type AS variableType," +
            " COUNT(*) AS instanceCount," +
            " COALESCE(SUM(pg_column_size(combined.\"value\")), 0) AS totalSizeBytes," +
            " COALESCE(AVG(pg_column_size(combined.\"value\")), 0) AS avgSizeBytes" +
            " FROM (" +
            "   SELECT 'PROCESS' AS source_table, process_definition_key, name, type, \"value\"" +
            "   FROM process_variable WHERE marked_as_deleted = false" +
            "   UNION ALL" +
            "   SELECT 'TASK' AS source_table, process_definition_key, name, type, \"value\"" +
            "   FROM task_variable WHERE marked_as_deleted = false" +
            " ) combined" +
            " LEFT JOIN process_definition pd" +
            "   ON pd.\"processDefinitionKey\" = combined.process_definition_key" +
            " GROUP BY combined.source_table, combined.process_definition_key," +
            "   pd.name, pd.version, combined.name, combined.type" +
            " ORDER BY totalSizeBytes DESC" +
            " LIMIT :limit",
        nativeQuery = true
    )
    List<StorageConsumerProjection> findTopStorageConsumers(@Param("limit") int limit);
}
