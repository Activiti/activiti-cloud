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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.app.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.specification.TaskSpecification;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * <h2>Push-based task counts, end to end, in one runnable file.</h2>
 *
 * A working POC of the whole read path, against a real database, with no Docker daemon and no broker. Run it
 * and read the assertions - between them they answer the three questions developers ask about this feature:
 *
 * <ol>
 *   <li><b>How do we get what we need out of a task event?</b>
 *       {@link #step1TheEventIsTheOnlySourceForARemovedGroup()}, {@link #step2NonTaskEventsCostNothing()}</li>
 *   <li><b>How do we get the count right?</b> {@link #step3OneCorrectCountPerAudience()},
 *       {@link #step4BothSqlShapesAgree(boolean)}, {@link #step6TheTwoWaysToGetTheCountWrong()}</li>
 *   <li><b>How do we change or add a query?</b> {@link #step7AddingYourOwnCountQuery()}</li>
 * </ol>
 *
 * <h3>The path being exercised</h3>
 * <pre>
 * events (committed batch)
 *   -&gt; TaskCountEmitter          reads task ids and named groups off the events
 *   -&gt; TaskCountRecomputer       asks the registry which audiences care, then runs one COUNT each
 *   -&gt; TaskCountChangePublisher  one message per audience   (here: collected into a list)
 * </pre>
 *
 * In production the emitter is triggered from {@code QueryConsumerMessageHandler.accept} through an
 * after-commit transaction synchronization, and the publisher writes to the {@code taskCountsProducer}
 * binding. Everything else below is the production code, including the beans.
 *
 * <h3>The one idea to take away</h3>
 * An event tells you a <em>task's</em> candidate groups. A count needs a <em>subscriber's</em> group set. Those
 * are not the same thing and neither can be derived from the other: {@code {eng}} and {@code {eng,hr}} are two
 * different numbers. That is why {@link SubscriberScopeRegistry} exists - the REST tier records each
 * subscriber's group set as it serves them a count, and this path recomputes only the group sets that were
 * actually recorded.
 */
@SpringBootTest(
    classes = PushedTaskCountPocConfiguration.class,
    properties = {
        "spring.main.banner-mode=off",
        // INIT: the variable tables map their value column as `columnDefinition = "jsonb"`, which is Postgres
        // syntax H2 does not know. Declaring JSONB as a domain over H2's own JSON type makes that DDL legal.
        // Without it Hibernate silently skips those two tables and the first task save fails on a missing
        // PROCESS_VARIABLE - which is also why halt_on_error is on below: schema errors should be loud.
        "spring.datasource.url=jdbc:h2:mem:pushed-task-count-poc;DB_CLOSE_DELAY=-1" +
            ";INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.jpa.open-in-view=false",
    }
)
class PushedTaskCountPocTest {

    private static final long NOW = 1_700_000_000_000L;

    private static final String ENG = "eng";
    private static final String HR = "hr";
    private static final String FINANCE = "finance";

    /** Named as an individual candidate user on {@link #TASK_FOR_ALICE_ALONE}, and a member of {@link #ENG}. */
    private static final String ALICE = "alice";

    /** The only standalone task in the fixture - used by step 3 to show a second filter counting differently. */
    private static final String TASK_FOR_ENG = "task-for-eng";

    private static final String TASK_FOR_ENG_AND_HR = "task-for-eng-and-hr";
    private static final String TASK_FOR_HR = "task-for-hr";
    private static final String TASK_FOR_EVERYONE = "task-for-everyone";
    private static final String TASK_FOR_ALICE_ALONE = "task-for-alice-alone";
    private static final String TASK_ALREADY_ASSIGNED = "task-already-assigned";
    private static final String TASK_ALREADY_COMPLETED = "task-already-completed";

    private static final String A_PROCESS_INSTANCE = "process-instance-1";

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    /**
     * The production recomputer bean, from {@code QueryRepositoryAutoConfiguration}. In a real deployment it
     * shares its {@link SubscriberScopeRegistry} with the REST tier, which is what makes the registry useful.
     */
    @Autowired
    private TaskCountRecomputer recomputer;

    @Autowired
    private SubscriberScopeRegistry registry;

    /** Stands in for {@code TaskCountsChannelPublisher}: same interface, keeps the messages instead of sending them. */
    private final List<TaskCountChangedEvent> published = new ArrayList<>();

    private TaskCountEmitter emitter;

    @BeforeEach
    void setUp() {
        published.clear();
        emitter = new TaskCountEmitter(
            recomputer,
            published::addAll,
            Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
        );

        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
        taskRepository.deleteAll();

        // The read model as the event handlers would have left it. Nothing here is push-specific: these are
        // ordinary query rows, written by the ordinary event handlers.
        queuedTask(TASK_FOR_ENG, null, ENG); // standalone: no process instance
        queuedTask(TASK_FOR_ENG_AND_HR, A_PROCESS_INSTANCE, ENG, HR);
        queuedTask(TASK_FOR_HR, A_PROCESS_INSTANCE, HR);

        // No candidate groups and no candidate users: every audience can see it - see step 2d.
        queuedTask(TASK_FOR_EVERYONE, A_PROCESS_INSTANCE);

        // Reachable only by naming alice personally, so no group scope can see it - see step 2d.
        queuedTask(TASK_FOR_ALICE_ALONE, A_PROCESS_INSTANCE);
        taskCandidateUserRepository.save(new TaskCandidateUserEntity(TASK_FOR_ALICE_ALONE, ALICE));

        // Excluded from the queued count: it has an assignee, so it is somebody's work, not the queue's.
        TaskEntity assigned = queuedTask(TASK_ALREADY_ASSIGNED, A_PROCESS_INSTANCE, ENG);
        assigned.setAssignee("bob");
        assigned.setStatus(Task.TaskStatus.ASSIGNED);
        taskRepository.save(assigned);

        // Excluded by the status filter.
        TaskEntity completed = queuedTask(TASK_ALREADY_COMPLETED, A_PROCESS_INSTANCE, ENG);
        completed.setStatus(Task.TaskStatus.COMPLETED);
        taskRepository.save(completed);
    }

    @AfterEach
    void tearDown() {
        FeatureToggleHolder.reset();
    }

    // ---------------------------------------------------------------------------------------------------
    // 1. Getting what we need out of the events
    // ---------------------------------------------------------------------------------------------------

    /**
     * The strongest demonstration that task ids and group ids come off the <em>events</em> rather than out of a
     * follow-up query: this scenario deletes the row linking the task to HR, and still produces a correct HR
     * count.
     * <p>
     * A {@code TASK_CANDIDATE_GROUP_REMOVED} event carries {@code getTaskId()} <em>and</em>
     * {@code getGroupId()}. By the time we recompute, the batch has committed and that row is gone - so the
     * event is the only thing left that knows HR was affected. Miss this and every "a task left my queue"
     * badge silently stops updating.
     */
    @Test
    @DisplayName("1a. A removed candidate group is only knowable from the event")
    void step1TheEventIsTheOnlySourceForARemovedGroup() {
        //given HR is listening
        registry.record(List.of(HR));

        //and the committed batch has already deleted the row that put this task in HR's queue
        taskCandidateGroupRepository.delete(new TaskCandidateGroupEntity(TASK_FOR_ENG_AND_HR, HR));

        //when the removal event is processed
        emitter.emitFor(List.of(candidateGroupRemoved(TASK_FOR_ENG_AND_HR, HR)));

        //then HR is told its queue shrank to 2, even though nothing in the database still associates the
        //departed task with HR
        assertThat(published)
            .singleElement()
            .satisfies(change -> {
                assertThat(change.scopeKey()).isEqualTo("groups:hr");
                assertThat(change.count()).isEqualTo(2); // TASK_FOR_HR + TASK_FOR_EVERYONE
            });
    }

    /**
     * The emitter pattern-matches on {@code event.getEntity()}. Anything that is not a {@code Task},
     * {@code TaskCandidateGroup} or {@code TaskCandidateUser} cannot move a task count, so it is dropped
     * before any transaction is opened - no database round trip at all.
     */
    @Test
    @DisplayName("1b. Non-task events are dropped before touching the database")
    void step2NonTaskEventsCostNothing() {
        registry.record(List.of(ENG));

        emitter.emitFor(List.of(new CloudProcessStartedEventImpl()));

        assertThat(published).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------
    // 2. Getting the count right
    // ---------------------------------------------------------------------------------------------------

    /**
     * One task changes; several audiences are listening; each gets its own correct number from its own COUNT
     * query. This is the point of the feature - the number is computed once <em>per group set</em>, not once
     * per user.
     *
     * <p>Given the queued tasks in {@link #setUp()}:
     * <pre>
     *   audience     sees                                                          count
     *   {eng}        task-for-eng, task-for-eng-and-hr, task-for-everyone             3
     *   {hr}         task-for-eng-and-hr, task-for-hr, task-for-everyone              3
     *   {eng,hr}     all four of the above                                            4
     *   {finance}    task-for-everyone                                                1
     * </pre>
     * {@code {eng}} and {@code {eng,hr}} disagree, and {@code {finance}} is not zero. Neither number can be
     * worked out from the other, which is why the registry has to remember real group sets.
     */
    @Test
    @DisplayName("2a. One correct count per audience, from one event")
    void step3OneCorrectCountPerAudience() {
        //given four audiences have fetched a count recently
        registry.record(List.of(ENG));
        registry.record(List.of(HR));
        registry.record(List.of(ENG, HR));
        registry.record(List.of(FINANCE));

        //when a task whose only candidate group is eng changes
        emitter.emitFor(List.of(taskCreated(TASK_FOR_ENG)));

        //then only the audiences that hold a candidacy on it are recomputed, each with its own number
        assertThat(published).containsExactlyInAnyOrder(
            new TaskCountChangedEvent("groups:eng", List.of(ENG), 3, NOW),
            new TaskCountChangedEvent("groups:eng,hr", List.of(ENG, HR), 4, NOW)
        );

        //and hr and finance are not queried at all: their queues cannot have changed, so there is nothing to
        //tell them. This is the saving - it is not "one query instead of 2000", it is also "no query for the
        //audiences that do not care".
        assertThat(published).extracting(TaskCountChangedEvent::scopeKey).doesNotContain("groups:hr", "groups:finance");
    }

    /**
     * {@code TaskSpecification} builds two different SQL shapes behind
     * {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} - {@code LEFT JOIN} + {@code isEmpty} when off,
     * correlated {@code EXISTS} subqueries when on. A count that changes when a performance toggle flips is a
     * bug, so both shapes are pinned to the same numbers here.
     */
    @ParameterizedTest(name = "2b. Same counts with EXISTS subqueries = {0}")
    @ValueSource(booleans = { false, true })
    void step4BothSqlShapesAgree(boolean existsSubqueriesEnabled) {
        FeatureToggleHolder.initialize(
            feature -> existsSubqueriesEnabled && QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES.equals(feature)
        );
        registry.record(List.of(ENG));
        registry.record(List.of(ENG, HR));

        emitter.emitFor(List.of(taskCreated(TASK_FOR_ENG)));

        assertThat(published)
            .extracting(TaskCountChangedEvent::scopeKey, TaskCountChangedEvent::count)
            .containsExactlyInAnyOrder(tuple("groups:eng", 3L), tuple("groups:eng,hr", 4L));
    }

    /**
     * Nothing recorded means nothing computed - not one query. Worth knowing when a badge appears to stop
     * updating: if the subscriber's group set has aged out of the registry (TTL
     * {@code query.count-scopes.registry.ttl}, 15 minutes by default) it receives nothing until it fetches a
     * count again. That TTL is a correctness setting, not a tuning knob.
     */
    @Test
    @DisplayName("2c. Nobody listening means no queries are run")
    void step5NothingIsComputedWhenNobodyIsListening() {
        emitter.emitFor(List.of(taskCreated(TASK_FOR_ENG)));

        assertThat(published).isEmpty();
    }

    /**
     * The two traps. Both are properties of a group-scoped count rather than bugs, and both produce a
     * plausible-looking wrong badge if ignored.
     *
     * <ol>
     *   <li><b>It is not additive with the per-user count.</b> Visibility is a union, not a partition: alice
     *       sees the group's tasks <em>and</em> her own. Adding the two numbers double-counts.</li>
     *   <li><b>It omits individually named candidates.</b> A task whose only link to alice is a
     *       {@code candidateUser} row is invisible to every group scope, so a pushed group count can be lower
     *       than what alice sees on screen.</li>
     * </ol>
     *
     * The mirror image of trap 2 is a task with no candidates at all, which every audience sees. That is why
     * {@code TaskCountRecomputer} falls back to {@code allGroupSets()} when it finds one, and why
     * {@code {finance}} counted 1 rather than 0 in step 2a.
     */
    @Test
    @DisplayName("2d. The two ways to get this wrong: additivity and individual candidates")
    void step6TheTwoWaysToGetTheCountWrong() {
        long groupCountForEng = taskRepository.count(
            TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of(ENG))
        );
        long whatAliceActuallySees = taskRepository.count(
            TaskSpecification.restricted(PushedTaskCountFilter.QUEUED, ALICE, List.of(ENG))
        );

        assertThat(groupCountForEng).isEqualTo(3);
        assertThat(whatAliceActuallySees).isEqualTo(4);

        //trap 1: a badge that added the pushed group count to alice's own count would show 7 rather than 4
        assertThat(groupCountForEng + whatAliceActuallySees).isEqualTo(7);

        //trap 2: the gap between the two is exactly the task that names alice individually
        assertThat(whatAliceActuallySees - groupCountForEng).isEqualTo(1);

        //and a group-scoped specification refuses to be built without groups, rather than silently counting
        //every task in the tenant
        assertThatThrownBy(() -> TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of())).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    // ---------------------------------------------------------------------------------------------------
    // 3. Changing or adding a query
    // ---------------------------------------------------------------------------------------------------

    /**
     * How to push a different count.
     * <p>
     * The filter is a {@link TaskSearchRequest} - the same payload the REST endpoint deserialises - so
     * "adding a query" means declaring one more constant and counting with it. Below, the pinned
     * {@link PushedTaskCountFilter#QUEUED} filter is narrowed to standalone tasks, and the number drops from 3
     * to 1 through exactly the same code path.
     * <p>
     * Two rules to respect when you add one:
     * <ul>
     *   <li><b>No user identity in the filter.</b> Assignee, candidate user, "my tasks" - anything
     *       per-person makes the count unshareable, and a count that serves one person is just the REST call
     *       again. Identity enters only through {@code forGroups(filter, groups)}.</li>
     *   <li><b>Producer and consumer must agree statically.</b> The consumer has no HTTP request to read a
     *       filter from, so both ends have to name the same constant. Push a second count and clients need to
     *       know which one a message carries - the {@code scopeKey} currently encodes only the audience, so a
     *       second filter means extending the key.</li>
     * </ul>
     */
    @Test
    @DisplayName("3. Adding your own pushed count is one more filter constant")
    void step7AddingYourOwnCountQuery() {
        TaskSearchRequest queuedStandaloneOnly = queuedFilterNarrowedToStandaloneTasks();

        assertThat(
            taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of(ENG)))
        ).isEqualTo(3);
        assertThat(taskRepository.count(TaskSpecification.forGroups(queuedStandaloneOnly, List.of(ENG)))).isEqualTo(1); // only TASK_FOR_ENG has no process instance
    }

    /**
     * A second pushed filter, written out the long way on purpose: {@link TaskSearchRequest} is a 29-component
     * record with no builder in this module, and naming every component is the clearest way to show that a
     * pushed filter is an ordinary search request with no identity in it.
     */
    //prettier-ignore
    private static TaskSearchRequest queuedFilterNarrowedToStandaloneTasks() {
        return new TaskSearchRequest(
            "pushed-queued-standalone-count", // requestId
            true,                             // onlyStandalone  <-- the only change from QUEUED
            false,                            // onlyRoot
            null, null, null, null, null, null, null,
            Set.of(Task.TaskStatus.CREATED),  // status
            null, null, null, null, null, null, null, null, null, null, null, null,
            null,                             // candidateUserId  - stays null: no identity in a shared count
            null,                             // candidateGroupId - restriction comes from forGroups(), not here
            null, null, null, null
        );
    }

    // ---------------------------------------------------------------------------------------------------
    // fixture helpers
    // ---------------------------------------------------------------------------------------------------

    /**
     * An unassigned, {@code CREATED} task - what {@link PushedTaskCountFilter#QUEUED} counts - plus its
     * candidate groups.
     *
     * @param processInstanceId {@code null} for a standalone task. Set at insert time because the column is
     *                          mapped {@code updatable = false}, so a later change would be silently dropped.
     */
    private TaskEntity queuedTask(String taskId, String processInstanceId, String... candidateGroups) {
        TaskEntity task = new TaskEntity();
        task.setId(taskId);
        task.setName(taskId);
        task.setStatus(Task.TaskStatus.CREATED);
        task.setCreatedDate(new Date());
        task.setProcessInstanceId(processInstanceId);
        TaskEntity saved = taskRepository.save(task);
        for (String group : candidateGroups) {
            taskCandidateGroupRepository.save(new TaskCandidateGroupEntity(taskId, group));
        }
        return saved;
    }

    private static CloudRuntimeEvent<?, ?> taskCreated(String taskId) {
        TaskImpl task = new TaskImpl();
        task.setId(taskId);
        return new CloudTaskCreatedEventImpl(task);
    }

    private static CloudRuntimeEvent<?, ?> candidateGroupRemoved(String taskId, String groupId) {
        return new CloudTaskCandidateGroupRemovedEventImpl(new TaskCandidateGroupImpl(groupId, taskId));
    }
}
