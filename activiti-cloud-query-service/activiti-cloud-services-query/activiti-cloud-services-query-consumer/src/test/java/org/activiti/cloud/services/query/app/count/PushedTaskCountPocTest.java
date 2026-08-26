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

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskCandidateGroupImpl;
import org.activiti.api.task.model.impl.TaskCandidateUserImpl;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateGroupRemovedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCandidateUserAddedEventImpl;
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
 *       {@link #step4BothSqlShapesAgree(boolean)}, {@link #step6TheTwoWaysToGetTheCountWrong()},
 *       {@link #step7ATaskWithBothCandidateGroupsAndUsers()},
 *       {@link #step7bOverlappingCandidateUsersAndGroupsAreCountedOnce(boolean)}</li>
 *   <li><b>How do we change or add a query?</b> {@link #step8AddingYourOwnCountQuery()}</li>
 * </ol>
 *
 * <p>If you only read one method, read {@link #step7ATaskWithBothCandidateGroupsAndUsers()}: it lays the
 * group-scoped query and the per-user query side by side on a task that has <em>both</em> a candidate group and
 * a candidate user, and shows why neither answer can be derived from the other.
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

    /**
     * Named as an individual candidate user on {@link #TASK_FOR_ALICE_ALONE} and {@link #TASK_FOR_ENG_AND_ALICE}.
     * Her group membership varies by scenario - she is asked about as a member of {@link #ENG} in step 2d and of
     * {@link #HR} in step 2e.
     */
    private static final String ALICE = "alice";

    /** A second individual, so step 2f can build a task with more than one candidate user. */
    private static final String BOB = "bob";

    /** The only standalone task in the fixture - used by step 3 to show a second filter counting differently. */
    private static final String TASK_FOR_ENG = "task-for-eng";

    private static final String TASK_FOR_ENG_AND_HR = "task-for-eng-and-hr";
    private static final String TASK_FOR_HR = "task-for-hr";
    private static final String TASK_FOR_EVERYONE = "task-for-everyone";
    private static final String TASK_FOR_ALICE_ALONE = "task-for-alice-alone";

    /** Both kinds of candidate at once: a candidate group <em>and</em> a candidate user - see step 2e. */
    private static final String TASK_FOR_ENG_AND_ALICE = "task-for-eng-and-alice";

    /**
     * Created inside step 2f rather than in the shared fixture, so that step can measure the counts before and
     * after inserting it. Two candidate groups and two candidate users, deliberately overlapping.
     */
    private static final String TASK_WITH_OVERLAPPING_CANDIDATES = "task-with-overlapping-candidates";

    private static final String TASK_ALREADY_ASSIGNED = "task-already-assigned";
    private static final String TASK_ALREADY_COMPLETED = "task-already-completed";

    private static final String A_PROCESS_INSTANCE = "process-instance-1";

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    /** Only used by {@link #joinedRowsFor(String)}, to count joined rows the repositories cannot express. */
    @Autowired
    private EntityManager entityManager;

    /**
     * The beans {@code QueryRepositoryAutoConfiguration} declares, injected so that a failure to wire them fails
     * this test class - see {@link #step0TheProductionBeanGraphResolves()}. In a real deployment the registry is
     * shared with the REST tier, which is what makes it useful at all.
     */
    @Autowired
    private TaskCountRecomputer productionRecomputer;

    @Autowired
    private SubscriberScopeRegistry productionRegistry;

    /** Stands in for {@code TaskCountsChannelPublisher}: same interface, keeps the messages instead of sending them. */
    private final List<TaskCountChangedEvent> published = new ArrayList<>();

    /**
     * Rebuilt for every test, with the same constructor arguments and defaults the auto-configuration uses.
     * <p>
     * The registry is a singleton in production - which is the point of it - so sharing the bean across these
     * tests would leak recorded group sets from one scenario into the next, and the assertions here are about
     * <em>exactly</em> which audiences get recomputed. A fresh instance per test is the only way to keep them
     * honest and order-independent. The one thing lost is the {@code REQUIRES_NEW} transaction proxy, which
     * changes nothing here because no test holds an ambient transaction: each {@code count(...)} already runs in
     * its own read-only one.
     */
    private SubscriberScopeRegistry registry;

    private TaskCountRecomputer recomputer;

    private TaskCountEmitter emitter;

    @BeforeEach
    void setUp() {
        published.clear();
        registry = new SubscriberScopeRegistry(Duration.ofMinutes(15), 10_000);
        recomputer = new TaskCountRecomputer(
            taskRepository,
            taskCandidateGroupRepository,
            taskCandidateUserRepository,
            registry,
            200
        );
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

        // Both kinds of candidate at once: eng holds it as a group queue item, and alice is also named on it
        // personally. The one task the group query and the user query genuinely disagree about - see step 2e.
        queuedTask(TASK_FOR_ENG_AND_ALICE, A_PROCESS_INSTANCE, ENG);
        taskCandidateUserRepository.save(new TaskCandidateUserEntity(TASK_FOR_ENG_AND_ALICE, ALICE));

        // Excluded from the queued count: it has an assignee, so it is somebody's work, not the queue's.
        TaskEntity assigned = queuedTask(TASK_ALREADY_ASSIGNED, A_PROCESS_INSTANCE, ENG);
        assigned.setAssignee(BOB);
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
    // 0. The wiring
    // ---------------------------------------------------------------------------------------------------

    /**
     * Cheap but worth having: until this ran, nothing had ever started a context containing
     * {@code taskCountRecomputer}, so a wiring mistake in {@code QueryRepositoryAutoConfiguration} would only
     * have surfaced on a real deployment. Injection failing is what fails this - the assertions are a formality.
     */
    @Test
    @DisplayName("0. The production bean graph resolves")
    void step0TheProductionBeanGraphResolves() {
        assertThat(productionRecomputer).isNotNull();
        assertThat(productionRegistry).isNotNull();
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
     *   audience     sees                                                                    count
     *   {eng}        task-for-eng, task-for-eng-and-hr, task-for-eng-and-alice,
     *                task-for-everyone                                                          4
     *   {hr}         task-for-eng-and-hr, task-for-hr, task-for-everyone                        3
     *   {eng,hr}     all five of the above                                                      5
     *   {finance}    task-for-everyone                                                          1
     * </pre>
     * {@code {eng}} and {@code {eng,hr}} disagree, and {@code {finance}} is not zero. Neither number can be
     * worked out from the other, which is why the registry has to remember real group sets.
     * <p>
     * Note what is <em>absent</em> from every row: {@code task-for-alice-alone}. No group scope can see it.
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
            new TaskCountChangedEvent("groups:eng", List.of(ENG), 4, NOW),
            new TaskCountChangedEvent("groups:eng,hr", List.of(ENG, HR), 5, NOW)
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
            .containsExactlyInAnyOrder(tuple("groups:eng", 4L), tuple("groups:eng,hr", 5L));
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

        assertThat(groupCountForEng).isEqualTo(4);
        assertThat(whatAliceActuallySees).isEqualTo(5);

        //trap 1: a badge that added the pushed group count to alice's own count would show 9 rather than 5
        assertThat(groupCountForEng + whatAliceActuallySees).isEqualTo(9);

        //trap 2: the gap between the two is exactly task-for-alice-alone, the one task alice can reach that no
        //eng-scoped count can. Step 2e shows the harder version, where the missing task does have candidate
        //groups - just not the audience's.
        assertThat(whatAliceActuallySees - groupCountForEng).isEqualTo(1);

        //and a group-scoped specification refuses to be built without groups, rather than silently counting
        //every task in the tenant
        assertThatThrownBy(() -> TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of())).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    /**
     * <h3>A task carrying both a candidate group and a candidate user - and the two queries it needs.</h3>
     *
     * {@code task-for-eng-and-alice} has candidate group {@code eng} and candidate user {@code alice}. Ask about
     * it as an HR member and the two queries disagree, because they match through different branches of
     * {@code TaskSpecification}:
     *
     * <pre>
     *   forGroups(QUEUED, {hr})              → assignee IS NULL AND ( candidateGroup IN {hr}
     *                                                               OR no candidates at all )
     *                                          → 3   does NOT include task-for-eng-and-alice
     *
     *   restricted(QUEUED, alice, {hr})      → assignee = alice OR owner = alice
     *                                          OR ( assignee IS NULL AND ( candidateUser = alice
     *                                                                    OR candidateGroup IN {hr}
     *                                                                    OR no candidates at all ) )
     *                                          → 5   DOES include it, via the candidateUser branch
     * </pre>
     *
     * So the same task sits <em>inside</em> the pushed count for {@code {eng}} and <em>outside</em> the pushed
     * count for {@code {hr}}, while alice sees it either way. The group query is not a subset-by-audience of the
     * user query; the two are different questions.
     *
     * <p>Note the "no candidates at all" branch needs <em>both</em> collections empty
     * ({@code isEmpty(taskCandidateUsers) AND isEmpty(taskCandidateGroups)}), so a task with both kinds of
     * candidate never reaches it - and never triggers the recomputer's {@code allGroupSets()} fallback either.
     *
     * <p><b>The consequence for this feature:</b> only the group query is pushed. A candidate-user event on this
     * task publishes group scopes and nothing else - alice's own number changed and no message says so. That is
     * not an oversight to patch here: {@code TaskCountEmitter.collect} keeps {@code TaskCandidateUser}'s
     * {@code getTaskId()} but discards {@code getUserId()}, so there is no per-user analogue of
     * {@code groupsNamedInBatch} - and a <em>removed</em> candidate user would be unrecoverable after commit for
     * exactly the reason step 1a exists. Pushing per-user counts means solving that first.
     */
    @Test
    @DisplayName("2e. A task with both candidate groups and users: two queries, two different answers")
    void step7ATaskWithBothCandidateGroupsAndUsers() {
        //the query that gets pushed, for an audience that does not hold this task's candidacy
        long pushedForHr = taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of(HR)));
        //the query alice's own badge is served from, with the same group membership
        long aliceInHr = taskRepository.count(
            TaskSpecification.restricted(PushedTaskCountFilter.QUEUED, ALICE, List.of(HR))
        );

        assertThat(pushedForHr).isEqualTo(3);
        assertThat(aliceInHr).isEqualTo(5);

        //the gap is the two tasks alice is named on personally - one of which has candidate groups of its own
        assertThat(aliceInHr - pushedForHr).isEqualTo(2);

        //and the very same task is counted by the audience that does hold its candidacy
        assertThat(
            taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of(ENG)))
        ).isEqualTo(4);

        //when a candidate-user change on that task is processed
        registry.record(List.of(ENG));
        registry.record(List.of(HR));
        emitter.emitFor(List.of(candidateUserAdded(TASK_FOR_ENG_AND_ALICE, ALICE)));

        //then what goes out is group-scoped only: eng, because the task's candidate group rows say so. There is
        //no user: scope in the output even though a user's visible count is what actually changed.
        assertThat(published).extracting(TaskCountChangedEvent::scopeKey).containsExactly("groups:eng");
    }

    /**
     * <h3>What happens when the candidate users overlap the candidate groups.</h3>
     *
     * The obvious worry - "alice is a candidate user <em>and</em> a member of a candidate group, does she get
     * counted twice?" - has three separate answers, and only the third one is a real hazard.
     *
     * <p><b>1. Group membership is never resolved by the query.</b> There is no user-to-groups table in the query
     * model and no join that could reach one. The subscriber's group list is an <em>input</em>: the REST tier
     * reads it off the caller's token, passes it to {@code forGroups(filter, groups)}, and records it in the
     * registry. "alice is in eng" is never computed here - it arrives as {@code List.of("eng")}. So an overlap
     * between a task's candidate users and its candidate groups is not even visible to the count; the two sets
     * are compared against different things.
     *
     * <p><b>2. The restriction is a predicate, not a sum.</b> The candidate-user branch, the candidate-group
     * branch and the no-candidates branch are {@code OR}-ed alternative <em>reasons</em> a task row qualifies. A
     * task that qualifies for three reasons still qualifies once. Overlap cannot inflate a count through the
     * predicate.
     *
     * <p><b>3. The real hazard is row multiplication, and it is already handled.</b> With
     * {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} off, both collections are reached by {@code LEFT
     * JOIN}, so the task below - 2 candidate groups x 2 candidate users - produces <b>4 rows for one task</b>, and
     * for an {@code {eng,hr}} audience 2 of those group rows match the {@code IN} clause. A plain
     * {@code COUNT(*)} would return 5 or 6 where the answer is 4. What prevents it:
     * {@code SpecificationSupport.toPredicate} calls {@code query.distinct(true)}, which Spring Data's
     * {@code getCountQuery} turns into {@code COUNT(DISTINCT task.id)}. With {@code EXISTS} subqueries there are
     * no duplicate rows to collapse, so {@code DISTINCT} is deliberately dropped - which is why this test runs
     * both shapes.
     *
     * <p><b>The one gap in that safety net</b>, worth knowing before you add a pushed filter: {@code distinct} is
     * only applied when {@code query.getGroupList()} is empty, and a process- or task-variable filter forces a
     * {@code GROUP BY}. That case is covered by a different mechanism - {@code @CountOverFullWindow} on
     * {@code TaskSpecification}, which makes {@code CustomizedJpaSpecificationExecutorImpl} swap the count for a
     * {@code COUNT(*) OVER ()} window function - so it is handled, but by a second code path this POC does not
     * exercise. {@link PushedTaskCountFilter#QUEUED} has no variable filters.
     *
     * <p><b>And the consequence for pushing counts</b>, which narrows what step 2e said: when a candidate user
     * <em>is</em> covered by one of the task's candidate groups, a change to that task <em>does</em> reach them,
     * because the task has candidate group rows and {@code groupSetsIntersecting} fires on them. The uncovered
     * case is therefore narrower than "any candidate user" - it is a user whose recorded group sets do not
     * intersect the task's candidate groups at all.
     */
    @ParameterizedTest(name = "2f. Overlapping candidate users and groups count once, EXISTS subqueries = {0}")
    @ValueSource(booleans = { false, true })
    void step7bOverlappingCandidateUsersAndGroupsAreCountedOnce(boolean existsSubqueriesEnabled) {
        FeatureToggleHolder.initialize(
            feature -> existsSubqueriesEnabled && QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES.equals(feature)
        );

        long engBefore = queuedCountForGroups(ENG);
        long engAndHrBefore = queuedCountForGroups(ENG, HR);
        long financeBefore = queuedCountForGroups(FINANCE);
        long aliceInEngBefore = taskRepository.count(
            TaskSpecification.restricted(PushedTaskCountFilter.QUEUED, ALICE, List.of(ENG))
        );

        //given one task carrying two candidate groups and two candidate users, with alice in both sets as far as
        //an eng-scoped subscriber is concerned: 2 x 2 = 4 joined rows for a single task
        queuedTask(TASK_WITH_OVERLAPPING_CANDIDATES, A_PROCESS_INSTANCE, ENG, HR);
        taskCandidateUserRepository.save(new TaskCandidateUserEntity(TASK_WITH_OVERLAPPING_CANDIDATES, ALICE));
        taskCandidateUserRepository.save(new TaskCandidateUserEntity(TASK_WITH_OVERLAPPING_CANDIDATES, BOB));

        //the duplication is real, not hypothetical: joining both collections the way the legacy shape does
        //yields four rows for this one task. This is the counterfactual the DISTINCT protects against.
        assertThat(joinedRowsFor(TASK_WITH_OVERLAPPING_CANDIDATES)).isEqualTo(4);

        //then every count goes up by exactly one task, whichever SQL shape is in use
        assertThat(queuedCountForGroups(ENG)).isEqualTo(engBefore + 1);

        //the interesting one: {eng,hr} matches this task through two candidate group rows, and still counts it
        //once. Without DISTINCT this would be +2.
        assertThat(queuedCountForGroups(ENG, HR)).isEqualTo(engAndHrBefore + 1);

        //and alice matches it through the candidate-user branch and the candidate-group branch at the same time,
        //which is also worth exactly one
        assertThat(
            taskRepository.count(TaskSpecification.restricted(PushedTaskCountFilter.QUEUED, ALICE, List.of(ENG)))
        ).isEqualTo(aliceInEngBefore + 1);

        //an unrelated audience is unaffected: having candidate groups at all keeps this task out of the
        //no-candidates branch that finance sees
        assertThat(queuedCountForGroups(FINANCE)).isEqualTo(financeBefore);
    }

    // ---------------------------------------------------------------------------------------------------
    // 3. Changing or adding a query
    // ---------------------------------------------------------------------------------------------------

    /**
     * How to push a different count.
     * <p>
     * The filter is a {@link TaskSearchRequest} - the same payload the REST endpoint deserialises - so
     * "adding a query" means declaring one more constant and counting with it. Below, the pinned
     * {@link PushedTaskCountFilter#QUEUED} filter is narrowed to standalone tasks, and the number drops from 4
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
    void step8AddingYourOwnCountQuery() {
        TaskSearchRequest queuedStandaloneOnly = queuedFilterNarrowedToStandaloneTasks();

        assertThat(
            taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of(ENG)))
        ).isEqualTo(4);
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

    /** The pushed count, for an audience holding exactly the given groups. */
    private long queuedCountForGroups(String... groups) {
        return taskRepository.count(TaskSpecification.forGroups(PushedTaskCountFilter.QUEUED, List.of(groups)));
    }

    /**
     * How many rows one task becomes once both candidate collections are joined - the same two associations
     * {@code TaskSpecification} reaches through {@code LEFT JOIN} in its legacy shape, counted here <em>without</em>
     * {@code DISTINCT} on purpose. Used by step 2f to show that the duplication is real and that the count is only
     * correct because something collapses it.
     */
    private long joinedRowsFor(String taskId) {
        return entityManager
            .createQuery(
                "select count(t) from Task t " +
                    "left join t.taskCandidateGroups g " +
                    "left join t.taskCandidateUsers u " +
                    "where t.id = :taskId",
                Long.class
            )
            .setParameter("taskId", taskId)
            .getSingleResult();
    }

    private static CloudRuntimeEvent<?, ?> taskCreated(String taskId) {
        TaskImpl task = new TaskImpl();
        task.setId(taskId);
        return new CloudTaskCreatedEventImpl(task);
    }

    private static CloudRuntimeEvent<?, ?> candidateGroupRemoved(String taskId, String groupId) {
        return new CloudTaskCandidateGroupRemovedEventImpl(new TaskCandidateGroupImpl(groupId, taskId));
    }

    private static CloudRuntimeEvent<?, ?> candidateUserAdded(String taskId, String userId) {
        return new CloudTaskCandidateUserAddedEventImpl(new TaskCandidateUserImpl(userId, taskId));
    }
}
