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
package org.activiti.cloud.services.query.app.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.QueryRestTestApplication;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Verifies the counting semantics of {@link TaskSpecification#forGroups(TaskSearchRequest, java.util.Collection)}
 * against a real database, under both states of {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES}.
 *
 * <p>The point of {@code forGroups} is that a single count can be computed for a whole group set and served to
 * every user holding it, so the tests assert two things: the count is correct in isolation, and it agrees with
 * the per-user {@link TaskSpecification#restricted} count for a user whose only route to a task is a group.
 * The one case where the two deliberately diverge — an individual {@code candidateUser} entry — is pinned down
 * by {@link #shouldUndercountAgainstAUserWhoIsAlsoAnIndividualCandidate(boolean)} so the limitation cannot
 * regress silently into looking correct.
 */
@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
class TaskSpecificationGroupCountIT {

    private static final String ENG = "eng";
    private static final String FINANCE = "finance";
    private static final String HR = "hr";

    /** Group set under test. Matches ENG and FINANCE candidacies, not HR. */
    private static final List<String> GROUP_SET = List.of(ENG, FINANCE);

    /** Individually named as a candidate user on two tasks. */
    private static final String ALICE = "alice";

    /** Reaches tasks only through {@link #GROUP_SET}: never assignee, owner, or candidate user. */
    private static final String BOB = "bob";

    private static final String SOMEONE_ELSE = "someone-else";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @BeforeEach
    void setUp() {
        seedData();
    }

    @AfterEach
    void cleanUp() {
        queryTestUtils.cleanUp();
        FeatureToggleHolder.reset();
    }

    private void seedData() {
        // --- visible to GROUP_SET ---
        // candidate group in the set
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateGroups(ENG).buildAndSave();
        // one of several candidate groups is in the set
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateGroups(FINANCE, HR)
            .buildAndSave();
        // no candidates at all -> visible to everyone
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave();
        // reachable by group AND by an individual candidate user: the group route alone makes it visible
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateGroups(ENG)
            .withTaskCandidateUsers(ALICE)
            .buildAndSave();
        // TWO candidate groups both in the set: the legacy join yields two rows, so this task proves the
        // outer DISTINCT is doing its job and the task is counted once rather than twice
        queryTestUtils
            .buildTask()
            .withStatus(Task.TaskStatus.CREATED)
            .withTaskCandidateGroups(ENG, FINANCE)
            .buildAndSave();

        // --- NOT visible to GROUP_SET ---
        // candidate group outside the set
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateGroups(HR).buildAndSave();
        // already assigned: an assigned task belongs to its assignee, not to the queue
        queryTestUtils.buildTask().withAssignee(SOMEONE_ELSE).withTaskCandidateGroups(ENG).buildAndSave();
        // only an individual candidate user, no group: invisible to a group-scoped count by construction
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).withTaskCandidateUsers(ALICE).buildAndSave();
    }

    private static final long EXPECTED_GROUP_SET_COUNT = 5;

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldCountTasksVisibleToTheGroupSet(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        long count = taskRepository.count(TaskSpecification.forGroups(anyTask(), GROUP_SET));

        assertThat(count).isEqualTo(EXPECTED_GROUP_SET_COUNT);
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldMatchThePerUserCountForAUserReachingTasksOnlyThroughGroups(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        long groupScoped = taskRepository.count(TaskSpecification.forGroups(anyTask(), GROUP_SET));
        long perUser = taskRepository.count(TaskSpecification.restricted(anyTask(), BOB, GROUP_SET));

        // This equality is the whole premise of sharing one count across a group set.
        assertThat(groupScoped).isEqualTo(perUser).isEqualTo(EXPECTED_GROUP_SET_COUNT);
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldUndercountAgainstAUserWhoIsAlsoAnIndividualCandidate(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        long groupScoped = taskRepository.count(TaskSpecification.forGroups(anyTask(), GROUP_SET));
        long perUser = taskRepository.count(TaskSpecification.restricted(anyTask(), ALICE, GROUP_SET));

        // Alice additionally sees the candidate-user-only task, so the group-scoped count is short by
        // exactly that one. Documented limitation, not a bug: a user with individual candidacies cannot be
        // served by a group-scoped count alone.
        assertThat(perUser).isEqualTo(groupScoped + 1);
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldExcludeGroupsOutsideTheSet(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        long hrOnly = taskRepository.count(TaskSpecification.forGroups(anyTask(), List.of(HR)));

        // 2 HR candidacies (one shared with FINANCE, one HR-only) + the no-candidate task
        assertThat(hrOnly).isEqualTo(3);
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldCountOnlyTheNoCandidateTaskForAnUnknownGroup(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        long unknownGroup = taskRepository.count(TaskSpecification.forGroups(anyTask(), List.of("no-such-group")));

        // A group nobody is a candidate for still sees the task that has no candidates at all.
        assertThat(unknownGroup).isEqualTo(1);
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldStayUnrestrictedForTheAdminTier(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        long all = taskRepository.count(TaskSpecification.unrestricted(anyTask()));

        // Guard regression check: the group branch must not leak into the unrestricted tier.
        assertThat(all).isEqualTo(8);
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void shouldCombineWithRequestFilters(boolean existsSubqueries) {
        applyToggle(existsSubqueries);

        TaskSearchRequest createdOnly = new TaskSearchRequestBuilder().withStatus(Task.TaskStatus.CREATED).build();

        long count = taskRepository.count(TaskSpecification.forGroups(createdOnly, GROUP_SET));

        // The restriction is ANDed with the ordinary filters; every visible task here is already CREATED.
        assertThat(count).isEqualTo(EXPECTED_GROUP_SET_COUNT);
    }

    private TaskSearchRequest anyTask() {
        return new TaskSearchRequestBuilder().build();
    }

    private static void applyToggle(boolean existsSubqueries) {
        if (existsSubqueries) {
            FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES::equals);
        } else {
            FeatureToggleHolder.initialize(name -> false);
        }
    }
}
