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
package org.activiti.cloud.services.query.rest.payload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.rest.RestrictedTaskCountCacheKey;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.Test;

class TaskSearchRequestTest {

    @Test
    void should_beEqualAndShareHashCode_forIdenticalRequests() {
        TaskSearchRequest first = new TaskSearchRequestBuilder()
            .withRequestId("poll-1")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();
        TaskSearchRequest second = new TaskSearchRequestBuilder()
            .withRequestId("poll-1")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void should_notBeEqual_whenRequestIdDiffers() {
        TaskSearchRequest firstPoll = new TaskSearchRequestBuilder()
            .withRequestId("poll-1")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();
        TaskSearchRequest secondPoll = new TaskSearchRequestBuilder()
            .withRequestId("poll-2")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();

        assertThat(firstPoll).isNotEqualTo(secondPoll);
    }

    @Test
    void should_notBeEqual_whenRequestIdGoesFromNullToNonNull() {
        TaskSearchRequest unidentified = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();
        TaskSearchRequest correlated = new TaskSearchRequestBuilder()
            .withRequestId("poll-1")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();

        assertThat(unidentified).isNotEqualTo(correlated);
    }

    @Test
    void should_notBeEqual_whenFilterFieldsDiffer() {
        TaskSearchRequest assigned = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.ASSIGNED).build();
        TaskSearchRequest completed = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.COMPLETED).build();

        assertThat(assigned).isNotEqualTo(completed);
    }

    @Test
    void should_produceEqualCountCacheKeys_whenOnlyRequestIdDiffers() {
        TaskSearchRequest firstPoll = new TaskSearchRequestBuilder()
            .withRequestId("poll-1")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();
        TaskSearchRequest secondPoll = new TaskSearchRequestBuilder()
            .withRequestId("poll-2")
            .withStatus(Task.TaskStatus.ASSIGNED)
            .build();

        RestrictedTaskCountCacheKey firstKey = new RestrictedTaskCountCacheKey("user", List.of("group-a"), firstPoll);
        RestrictedTaskCountCacheKey secondKey = new RestrictedTaskCountCacheKey("user", List.of("group-a"), secondPoll);

        assertThat(firstKey).isEqualTo(secondKey).hasSameHashCodeAs(secondKey);
    }
}
