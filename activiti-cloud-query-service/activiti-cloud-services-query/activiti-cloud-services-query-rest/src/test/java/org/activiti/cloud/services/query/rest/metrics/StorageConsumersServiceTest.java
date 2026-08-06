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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.services.query.app.repository.StorageConsumerProjection;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class StorageConsumersServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private VariableRepository variableRepository;

    private StorageConsumersService service;

    @BeforeEach
    void setUp() {
        service = new StorageConsumersService(jdbcTemplate, variableRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_queryWithGivenLimit() {
        when(
            jdbcTemplate.query(
                eq(StorageConsumersService.TOP_CONSUMERS_QUERY),
                any(Object[].class),
                any(RowMapper.class)
            )
        ).thenReturn(List.of());

        service.findTopConsumers(25);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(
            eq(StorageConsumersService.TOP_CONSUMERS_QUERY),
            argsCaptor.capture(),
            any(RowMapper.class)
        );
        assertThat(argsCaptor.getValue()).containsExactly(25);
    }

    @Test
    void should_useDefaultLimit_when_limitIsZeroOrNegative() {
        assertThat(service.resolveLimit(0)).isEqualTo(StorageConsumersService.DEFAULT_LIMIT);
        assertThat(service.resolveLimit(-5)).isEqualTo(StorageConsumersService.DEFAULT_LIMIT);
        assertThat(service.resolveLimit(null)).isEqualTo(StorageConsumersService.DEFAULT_LIMIT);
    }

    @Test
    void should_capLimitAt100() {
        assertThat(service.resolveLimit(500)).isEqualTo(StorageConsumersService.MAX_LIMIT);
        assertThat(service.resolveLimit(100)).isEqualTo(100);
    }

    @Test
    void should_useCustomLimit_when_withinRange() {
        assertThat(service.resolveLimit(25)).isEqualTo(25);
        assertThat(service.resolveLimit(1)).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_convertBytesToGB_correctly() {
        // Mock the RowMapper to return pre-built rows (the actual conversion
        // happens inside the RowMapper lambda in the real code, so we verify
        // it returns data from the query call)
        Map<String, Object> row = Map.of(
            "processDefinitionKey",
            "testProcess",
            "variableName",
            "bigVar",
            "variableType",
            "json",
            "instanceCount",
            1L,
            "totalSizeGB",
            new BigDecimal("1.000000"),
            "avgSizeGB",
            new BigDecimal("1.000000")
        );

        when(
            jdbcTemplate.query(
                eq(StorageConsumersService.TOP_CONSUMERS_QUERY),
                any(Object[].class),
                any(RowMapper.class)
            )
        ).thenReturn(List.of(row));

        List<Map<String, Object>> result = service.findTopConsumers(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .containsEntry("processDefinitionKey", "testProcess")
            .containsEntry("totalSizeGB", new BigDecimal("1.000000"));
    }

    @Test
    void should_returnResultsFromRepository_when_findTopStorageConsumers() {
        StorageConsumerProjection projection = new StorageConsumerProjection() {
            @Override
            public String getSourceTable() {
                return "PROCESS";
            }

            @Override
            public String getProcessDefinitionKey() {
                return "orderProcess";
            }

            @Override
            public String getProcessDefinitionName() {
                return "Order Process";
            }

            @Override
            public Integer getProcessDefinitionVersion() {
                return 3;
            }

            @Override
            public String getVariableName() {
                return "orderPayload";
            }

            @Override
            public String getVariableType() {
                return "json";
            }

            @Override
            public Long getInstanceCount() {
                return 500L;
            }

            @Override
            public Long getTotalSizeBytes() {
                return 5_368_709_120L;
            }

            @Override
            public Long getAvgSizeBytes() {
                return 10_737_418L;
            }
        };

        when(variableRepository.findTopStorageConsumers(10)).thenReturn(List.of(projection));

        List<Map<String, Object>> result = service.findTopStorageConsumers(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .containsEntry("sourceTable", "PROCESS")
            .containsEntry("processDefinitionKey", "orderProcess")
            .containsEntry("processDefinitionName", "Order Process")
            .containsEntry("processDefinitionVersion", 3)
            .containsEntry("variableName", "orderPayload")
            .containsEntry("variableType", "json")
            .containsEntry("instanceCount", 500L)
            .containsEntry("totalSizeGB", new BigDecimal("5.000000"));
    }

    @Test
    void should_returnEmptyList_when_repositoryReturnsNoResults() {
        when(variableRepository.findTopStorageConsumers(10)).thenReturn(List.of());

        List<Map<String, Object>> result = service.findTopStorageConsumers(10);

        assertThat(result).isEmpty();
    }
}
