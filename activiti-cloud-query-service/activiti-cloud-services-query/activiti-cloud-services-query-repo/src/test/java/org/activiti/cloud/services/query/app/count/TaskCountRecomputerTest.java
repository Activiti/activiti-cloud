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
package org.activiti.cloud.services.query.app.count;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskCountRecomputerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Mock
    private TaskCandidateUserRepository taskCandidateUserRepository;

    private SubscriberScopeRegistry registry;
    private TaskCountRecomputer recomputer;

    @BeforeEach
    void setUp() {
        registry = new SubscriberScopeRegistry(Duration.ofMinutes(5), 100);
        recomputer = new TaskCountRecomputer(
            taskRepository,
            taskCandidateGroupRepository,
            taskCandidateUserRepository,
            registry,
            200
        );
    }

    @Test
    void shouldDoNothingWhenThereIsNothingToRecomputeFrom() {
        registry.record(List.of("eng"));

        assertThat(recomputer.recompute(Set.of(), Set.of())).isEmpty();

        verifyNoInteractions(taskRepository, taskCandidateGroupRepository, taskCandidateUserRepository);
    }

    @Test
    void shouldNotTouchTheDatabaseWhenNobodyIsListening() {
        assertThat(recomputer.recompute(Set.of("task-1"), Set.of())).isEmpty();

        verifyNoInteractions(taskRepository, taskCandidateGroupRepository, taskCandidateUserRepository);
    }

    @Test
    void shouldRecomputeOneCountPerIntersectingAudience() {
        registry.record(List.of("eng"));
        registry.record(List.of("eng", "hr"));
        registry.record(List.of("finance"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "eng"))
        );
        given(taskRepository.count(any(Specification.class))).willReturn(4L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), Set.of());

        assertThat(changes)
            .extracting(TaskCountChange::scopeKey)
            .containsExactlyInAnyOrder("groups:eng", "groups:eng,hr");
        assertThat(changes).allMatch(change -> change.count() == 4L);
        verify(taskRepository, times(2)).count(any(TaskSpecification.class));
    }

    @Test
    void shouldRecomputeForAGroupOnlyNamedByTheEventsWhenItsRowIsAlreadyGone() {
        registry.record(List.of("eng"));
        // The candidate row was deleted in the committed batch, so nothing links the task to eng any more.
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "hr"))
        );
        given(taskRepository.count(any(Specification.class))).willReturn(2L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), Set.of("eng"));

        assertThat(changes).extracting(TaskCountChange::scopeKey).containsExactly("groups:eng");
    }

    @Test
    void shouldRecomputeEveryAudienceWhenATaskHasNoCandidatesAtAll() {
        registry.record(List.of("eng"));
        registry.record(List.of("finance"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(Set.of());
        given(taskCandidateUserRepository.findByTaskIdIn(anyCollection())).willReturn(Set.of());
        given(taskRepository.count(any(Specification.class))).willReturn(9L);

        List<TaskCountChange> changes = recomputer.recompute(Set.of("task-1"), Set.of());

        // Such a task matches the specification's "no candidate users or groups" branch, which every
        // audience sees, so there is no set of groups to narrow by.
        assertThat(changes)
            .extracting(TaskCountChange::scopeKey)
            .containsExactlyInAnyOrder("groups:eng", "groups:finance");
    }

    @Test
    void shouldNotTreatACandidateUserOnlyTaskAsVisibleToEveryAudience() {
        registry.record(List.of("eng"));
        given(taskCandidateGroupRepository.findByTaskIdIn(Set.of("task-1"))).willReturn(Set.of());
        given(taskCandidateUserRepository.findByTaskIdIn(anyCollection())).willReturn(
            Set.of(new TaskCandidateUserEntity("task-1", "alice"))
        );

        assertThat(recomputer.recompute(Set.of("task-1"), Set.of())).isEmpty();

        verify(taskRepository, never()).count(any(Specification.class));
    }

    @Test
    void shouldNotRecomputeWhenNoRecordedAudienceIsAffected() {
        registry.record(List.of("eng"));
        given(taskCandidateGroupRepository.findByTaskIdIn(anySet())).willReturn(
            Set.of(new TaskCandidateGroupEntity("task-1", "legal"))
        );

        assertThat(recomputer.recompute(Set.of("task-1"), Set.of())).isEmpty();

        verify(taskRepository, never()).count(any(Specification.class));
    }
}
