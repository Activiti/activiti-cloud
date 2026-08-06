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
package org.activiti.cloud.services.query.rest.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.services.query.app.repository.StorageConsumerProjection;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Queries the process_variable table to find the top N variable groups
 * consuming the most storage. Uses pg_column_size() for exact on-disk
 * jsonb sizes, grouped by (processDefinitionKey, variableName, variableType).
 *
 * Provides two approaches:
 * <ul>
 *   <li>{@link #findTopConsumers(int)} - JdbcTemplate-based (process variables only)</li>
 *   <li>{@link #findTopStorageConsumers(int)} - Repository-based with @Query
 *       (process + task variables, with process definition details)</li>
 * </ul>
 */
public class StorageConsumersService {

    private static final BigDecimal BYTES_PER_GB = new BigDecimal(1_073_741_824L);

    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 100;

    /**
     * Native SQL using pg_column_size("value") to get the exact on-disk
     * size of each jsonb value column. Groups rows by process definition key,
     * variable name, and type, then orders by total size descending.
     * The LIMIT is parameterised so callers can control result count.
     */
    static final String TOP_CONSUMERS_QUERY =
        "SELECT process_definition_key," +
        " name," +
        " type," +
        " COUNT(*) AS instance_count," +
        " COALESCE(SUM(pg_column_size(\"value\")), 0) AS total_size_bytes," +
        " COALESCE(AVG(pg_column_size(\"value\")), 0) AS avg_size_bytes" +
        " FROM process_variable" +
        " WHERE marked_as_deleted = false" +
        " GROUP BY process_definition_key, name, type" +
        " ORDER BY total_size_bytes DESC" +
        " LIMIT ?";

    private final JdbcTemplate jdbcTemplate;
    private final VariableRepository variableRepository;

    public StorageConsumersService(JdbcTemplate jdbcTemplate, VariableRepository variableRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.variableRepository = variableRepository;
    }

    public List<Map<String, Object>> findTopConsumers(int limit) {
        int effectiveLimit = resolveLimit(limit);
        return jdbcTemplate.query(TOP_CONSUMERS_QUERY, new Object[] { effectiveLimit }, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("processDefinitionKey", rs.getString("process_definition_key"));
            row.put("variableName", rs.getString("name"));
            row.put("variableType", rs.getString("type"));
            row.put("instanceCount", rs.getLong("instance_count"));
            row.put("totalSizeGB", toGB(rs.getLong("total_size_bytes")));
            row.put("avgSizeGB", toGB(rs.getLong("avg_size_bytes")));
            return row;
        });
    }

    /**
     * Repository-based approach that queries both process_variable and
     * task_variable tables via UNION ALL, with process definition details
     * (name, version) from a LEFT JOIN to process_definition.
     */
    public List<Map<String, Object>> findTopStorageConsumers(int limit) {
        int effectiveLimit = resolveLimit(limit);
        List<StorageConsumerProjection> projections = variableRepository.findTopStorageConsumers(effectiveLimit);
        return projections
            .stream()
            .map(p -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("sourceTable", p.getSourceTable());
                row.put("processDefinitionKey", p.getProcessDefinitionKey());
                row.put("processDefinitionName", p.getProcessDefinitionName());
                row.put("processDefinitionVersion", p.getProcessDefinitionVersion());
                row.put("variableName", p.getVariableName());
                row.put("variableType", p.getVariableType());
                row.put("instanceCount", p.getInstanceCount());
                row.put("totalSizeGB", toGB(p.getTotalSizeBytes()));
                row.put("avgSizeGB", toGB(p.getAvgSizeBytes()));
                return row;
            })
            .toList();
    }

    int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private BigDecimal toGB(long bytes) {
        return new BigDecimal(bytes).divide(BYTES_PER_GB, 6, RoundingMode.HALF_UP);
    }
}
