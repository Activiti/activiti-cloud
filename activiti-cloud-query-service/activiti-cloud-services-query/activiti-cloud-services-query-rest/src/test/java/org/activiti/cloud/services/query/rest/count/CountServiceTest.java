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
package org.activiti.cloud.services.query.rest.count;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.activiti.cloud.services.query.rest.ProcessInstanceSearchService;
import org.activiti.cloud.services.query.rest.TaskControllerHelper;
import org.activiti.cloud.services.query.rest.payload.BatchCountRequest;
import org.activiti.cloud.services.query.app.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.rest.payload.ResourceType;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class CountServiceTest {

    private static final JsonMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .build();

    private TaskControllerHelper taskControllerHelper;
    private ProcessInstanceSearchService processInstanceSearchService;
    private CountService countService;

    @BeforeEach
    void setUp() {
        taskControllerHelper = mock(TaskControllerHelper.class);
        processInstanceSearchService = mock(ProcessInstanceSearchService.class);
        countService = new CountService(taskControllerHelper, processInstanceSearchService);
    }

    private TaskSearchRequest task(String requestId, String... statuses) {
        return MAPPER.convertValue(filterMap(requestId, statuses), TaskSearchRequest.class);
    }

    private ProcessInstanceSearchRequest processInstance(String requestId, String... statuses) {
        return MAPPER.convertValue(filterMap(requestId, statuses), ProcessInstanceSearchRequest.class);
    }

    private Map<String, Object> filterMap(String requestId, String... statuses) {
        Map<String, Object> map = new HashMap<>();
        if (requestId != null) {
            map.put("requestId", requestId);
        }
        map.put("status", Set.of(statuses));
        return map;
    }

    @Test
    void should_countRestricted_keyedByRequestId_perResourceType() {
        when(taskControllerHelper.countTasksRestricted(any())).thenReturn(3L, 5L);
        when(processInstanceSearchService.countRestricted(any())).thenReturn(7L);

        BatchCountRequest request = new BatchCountRequest(
            List.of(task("requestId1", "ASSIGNED"), task("requestId2", "CREATED")),
            List.of(processInstance("requestId3", "RUNNING"))
        );

        Map<ResourceType, Map<String, Long>> result = countService.countRestricted(request);

        assertThat(result.get(ResourceType.TASK)).containsExactly(entry("requestId1", 3L), entry("requestId2", 5L));
        assertThat(result.get(ResourceType.PROCESS_INSTANCE)).containsExactly(entry("requestId3", 7L));
        verify(taskControllerHelper, never()).countTasksUnrestricted(any());
        verify(processInstanceSearchService, never()).countUnrestricted(any());
    }

    @Test
    void should_countFilterWithMultipleStatuses_keyedByRequestId() {
        when(taskControllerHelper.countTasksRestricted(any())).thenReturn(8L);

        BatchCountRequest request = new BatchCountRequest(List.of(task("requestId1", "ASSIGNED", "SUSPENDED")), null);

        Map<ResourceType, Map<String, Long>> result = countService.countRestricted(request);

        assertThat(result.get(ResourceType.TASK)).containsExactly(entry("requestId1", 8L));
    }

    @Test
    void should_countUnrestricted_whenNotRestricted() {
        when(taskControllerHelper.countTasksUnrestricted(any())).thenReturn(11L);

        BatchCountRequest request = new BatchCountRequest(List.of(task("requestId1", "ASSIGNED")), null);

        Map<ResourceType, Map<String, Long>> result = countService.countUnrestricted(request);

        assertThat(result.get(ResourceType.TASK)).containsExactly(entry("requestId1", 11L));
        verify(taskControllerHelper, never()).countTasksRestricted(any());
    }

    @Test
    void should_countProcessInstancesUnrestricted_whenOnlyProcessInstancesRequested() {
        when(processInstanceSearchService.countUnrestricted(any())).thenReturn(9L);

        BatchCountRequest request = new BatchCountRequest(null, List.of(processInstance("requestId1", "RUNNING")));

        Map<ResourceType, Map<String, Long>> result = countService.countUnrestricted(request);

        assertThat(result).containsOnlyKeys(ResourceType.PROCESS_INSTANCE);
        assertThat(result.get(ResourceType.PROCESS_INSTANCE)).containsExactly(entry("requestId1", 9L));
        verify(processInstanceSearchService, never()).countRestricted(any());
        verify(taskControllerHelper, never()).countTasksUnrestricted(any());
    }

    @Test
    void should_throw_whenRequestNull() {
        assertThatThrownBy(() -> countService.countRestricted(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("At least one resource type");
    }

    @Test
    void should_throw_whenNoFiltersProvided() {
        BatchCountRequest request = new BatchCountRequest(null, null);

        assertThatThrownBy(() -> countService.countRestricted(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("At least one resource type");
    }

    @Test
    void should_throw_whenFilterHasNoRequestId() {
        BatchCountRequest request = new BatchCountRequest(List.of(task(null, "ASSIGNED")), null);

        assertThatThrownBy(() -> countService.countRestricted(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must specify a requestId");
    }

    @Test
    void should_throw_whenDuplicateRequestId() {
        BatchCountRequest request = new BatchCountRequest(
            List.of(task("dup", "CREATED"), task("dup", "ASSIGNED")),
            null
        );

        assertThatThrownBy(() -> countService.countRestricted(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate requestId");
    }
}
