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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class StorageConsumersControllerTest {

    @Mock
    private StorageConsumersService storageConsumersService;

    private StorageConsumersController controller;

    @BeforeEach
    void setUp() {
        controller = new StorageConsumersController(storageConsumersService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTopConsumers_when_serviceSucceeds() {
        Map<String, Object> row = Map.of(
            "processDefinitionKey",
            "orderProcess",
            "variableName",
            "orderPayload",
            "variableType",
            "json",
            "instanceCount",
            500L,
            "totalSizeGB",
            new BigDecimal("5.000000"),
            "avgSizeGB",
            new BigDecimal("0.010000")
        );

        when(storageConsumersService.findTopConsumers(10)).thenReturn(List.of(row));
        when(storageConsumersService.resolveLimit(10)).thenReturn(10);

        Map<String, Object> result = controller.storageConsumers(10);

        assertThat(result).containsKey("topConsumers");
        assertThat(result).doesNotContainKey("error");
        assertThat(result).containsEntry("limit", 10);

        List<Map<String, Object>> consumers = (List<Map<String, Object>>) result.get("topConsumers");
        assertThat(consumers).hasSize(1);
        assertThat(consumers.get(0))
            .containsEntry("processDefinitionKey", "orderProcess")
            .containsEntry("variableName", "orderPayload")
            .containsEntry("variableType", "json")
            .containsEntry("instanceCount", 500L);
    }

    @Test
    void should_returnEmptyListAndError_when_serviceFails() {
        when(storageConsumersService.findTopConsumers(anyInt())).thenThrow(
            new DataAccessException("connection refused") {}
        );

        Map<String, Object> result = controller.storageConsumers(10);

        assertThat(result).containsEntry("error", "connection refused");

        List<?> consumers = (List<?>) result.get("topConsumers");
        assertThat(consumers).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnEmptyList_when_noVariablesExist() {
        when(storageConsumersService.findTopConsumers(10)).thenReturn(List.of());
        when(storageConsumersService.resolveLimit(10)).thenReturn(10);

        Map<String, Object> result = controller.storageConsumers(10);

        assertThat(result).containsKey("topConsumers");
        assertThat(result).doesNotContainKey("error");

        List<Map<String, Object>> consumers = (List<Map<String, Object>>) result.get("topConsumers");
        assertThat(consumers).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTopStorageConsumerVariables_when_serviceSucceeds() {
        Map<String, Object> row = Map.of(
            "sourceTable",
            "PROCESS",
            "processDefinitionKey",
            "orderProcess",
            "processDefinitionName",
            "Order Process",
            "processDefinitionVersion",
            3,
            "variableName",
            "orderPayload",
            "variableType",
            "json",
            "instanceCount",
            500L,
            "totalSizeGB",
            new BigDecimal("5.000000"),
            "avgSizeGB",
            new BigDecimal("0.010000")
        );

        when(storageConsumersService.findTopStorageConsumers(10)).thenReturn(List.of(row));
        when(storageConsumersService.resolveLimit(10)).thenReturn(10);

        Map<String, Object> result = controller.storageConsumerVariables(10);

        assertThat(result).containsKey("topConsumers");
        assertThat(result).doesNotContainKey("error");
        assertThat(result).containsEntry("limit", 10);

        List<Map<String, Object>> consumers = (List<Map<String, Object>>) result.get("topConsumers");
        assertThat(consumers).hasSize(1);
        assertThat(consumers.get(0))
            .containsEntry("sourceTable", "PROCESS")
            .containsEntry("processDefinitionKey", "orderProcess")
            .containsEntry("processDefinitionName", "Order Process")
            .containsEntry("variableName", "orderPayload");
    }

    @Test
    void should_returnEmptyListAndError_when_storageConsumerVariablesFails() {
        when(storageConsumersService.findTopStorageConsumers(anyInt())).thenThrow(
            new DataAccessException("connection refused") {}
        );

        Map<String, Object> result = controller.storageConsumerVariables(10);

        assertThat(result).containsEntry("error", "connection refused");

        List<?> consumers = (List<?>) result.get("topConsumers");
        assertThat(consumers).isEmpty();
    }
}
