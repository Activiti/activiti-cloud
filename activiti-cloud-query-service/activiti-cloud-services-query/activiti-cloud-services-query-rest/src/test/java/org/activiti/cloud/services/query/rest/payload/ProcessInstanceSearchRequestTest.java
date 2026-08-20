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

import java.util.Set;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.rest.RestrictedProcessInstanceCountCacheKey;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.junit.jupiter.api.Test;

class ProcessInstanceSearchRequestTest {

    @Test
    void should_beEqualAndShareHashCode_forIdenticalRequests() {
        ProcessInstanceSearchRequest first = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .withNames("process-a")
            .build();
        ProcessInstanceSearchRequest second = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .withNames("process-a")
            .build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void should_notBeEqual_whenFilterFieldsDiffer() {
        ProcessInstanceSearchRequest running = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .build();
        ProcessInstanceSearchRequest completed = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.COMPLETED)
            .build();

        assertThat(running).isNotEqualTo(completed);
    }

    @Test
    void should_notBeEqual_whenSubprocessParentIdsDiffer() {
        ProcessInstanceSearchRequest first = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .build();
        ProcessInstanceSearchRequest second = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .build();
        second.setSubprocessParentIds(Set.of("parent-1", "parent-2"));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void should_produceEqualCountCacheKeys_forIdenticalRequests() {
        ProcessInstanceSearchRequest first = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .build();
        ProcessInstanceSearchRequest second = new ProcessInstanceSearchRequestBuilder()
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .build();

        RestrictedProcessInstanceCountCacheKey firstKey = new RestrictedProcessInstanceCountCacheKey("user", first);
        RestrictedProcessInstanceCountCacheKey secondKey = new RestrictedProcessInstanceCountCacheKey("user", second);

        assertThat(firstKey).isEqualTo(secondKey).hasSameHashCodeAs(secondKey);
    }
}
