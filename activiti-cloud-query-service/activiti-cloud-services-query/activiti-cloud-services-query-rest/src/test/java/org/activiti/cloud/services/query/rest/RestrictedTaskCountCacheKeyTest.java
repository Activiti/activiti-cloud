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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class RestrictedTaskCountCacheKeyTest {

    private static final String USER = "user";
    private static final List<String> GROUPS = List.of("group-a");
    private static final Date DATE_A = new Date(1_000L);
    private static final Date DATE_B = new Date(2_000L);
    private static final VariableFilter TASK_FILTER = new VariableFilter(
        "pdk",
        "tvar",
        VariableType.STRING,
        "a",
        FilterOperator.EQUALS
    );
    private static final VariableFilter PROCESS_FILTER = new VariableFilter(
        "pdk",
        "pvar",
        VariableType.STRING,
        "a",
        FilterOperator.EQUALS
    );
    private static final ProcessVariableKey PROCESS_KEY = new ProcessVariableKey("pdk", "key");
    private static final CloudRuntimeEntitySort SORT = new CloudRuntimeEntitySort(
        "field",
        Sort.Direction.ASC,
        false,
        null,
        null
    );

    @Test
    void should_produceEqualCountCacheKeys_whenOnlyRequestIdDiffers() {
        RestrictedTaskCountCacheKey firstKey = new RestrictedTaskCountCacheKey(USER, GROUPS, fullyPopulated("poll-1"));
        RestrictedTaskCountCacheKey secondKey = new RestrictedTaskCountCacheKey(USER, GROUPS, fullyPopulated("poll-2"));

        assertThat(firstKey).isEqualTo(secondKey).hasSameHashCodeAs(secondKey);
    }

    @Test
    void should_produceDifferentCountCacheKeys_whenABooleanFieldDiffers() {
        TaskSearchRequest standalone = new TaskSearchRequestBuilder().onlyStandalone().build();
        TaskSearchRequest notStandalone = new TaskSearchRequestBuilder().build();

        RestrictedTaskCountCacheKey standaloneKey = new RestrictedTaskCountCacheKey(USER, GROUPS, standalone);
        RestrictedTaskCountCacheKey notStandaloneKey = new RestrictedTaskCountCacheKey(USER, GROUPS, notStandalone);

        assertThat(standaloneKey).isNotEqualTo(notStandaloneKey);
    }

    @Test
    void should_produceDifferentCountCacheKeys_whenAFilterFieldDiffers() {
        TaskSearchRequest assigned = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();
        TaskSearchRequest completed = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.COMPLETED).build();

        RestrictedTaskCountCacheKey assignedKey = new RestrictedTaskCountCacheKey(USER, GROUPS, assigned);
        RestrictedTaskCountCacheKey completedKey = new RestrictedTaskCountCacheKey(USER, GROUPS, completed);

        assertThat(assignedKey).isNotEqualTo(completedKey);
    }

    private static TaskSearchRequest fullyPopulated(String requestId) {
        return new TaskSearchRequestBuilder()
            .withRequestId(requestId)
            .onlyStandalone()
            .onlyRoot()
            .withId("id")
            .withParentId("parent")
            .withProcessInstanceId("pi")
            .withName("name")
            .withDescription("desc")
            .withProcessDefinitionName("pdn")
            .withPriority(1)
            .withStatus(Task.TaskStatus.ASSIGNED)
            .withCompletedBy("completedBy")
            .withAssignees("assignee")
            .withCreatedFrom(DATE_A)
            .withCreatedTo(DATE_B)
            .withLastModifiedFrom(DATE_A)
            .withLastModifiedTo(DATE_B)
            .withLastClaimedFrom(DATE_A)
            .withLastClaimedTo(DATE_B)
            .withDueDateFrom(DATE_A)
            .withDueDateTo(DATE_B)
            .withCompletedFrom(DATE_A)
            .withCompletedTo(DATE_B)
            .withCandidateUserId("candidateUser")
            .withCandidateGroupId("candidateGroup")
            .withTaskVariableFilters(TASK_FILTER)
            .withProcessVariableFilters(PROCESS_FILTER)
            .withProcessVariableKeys(PROCESS_KEY)
            .withSort(SORT)
            .build();
    }
}
