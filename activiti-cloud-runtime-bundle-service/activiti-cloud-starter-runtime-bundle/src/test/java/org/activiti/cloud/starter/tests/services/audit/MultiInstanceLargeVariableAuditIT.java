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
package org.activiti.cloud.starter.tests.services.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.events.listeners.MessageProducerCommandContextCloseListener;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Measures what a parallel multi-instance subprocess over a large JSON variable does to the
 * per-CommandContext cloud-event list built by {@code ProcessEngineEventsAggregator}.
 *
 * <p>{@code BaseCommandContextEventsAggregator.add} appends every cloud event to an unbounded
 * {@link java.util.ArrayList} held as a CommandContext attribute
 * ({@code MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS}), and
 * {@code MessageProducerCommandContextCloseListener.closed} serialises the whole list into a
 * single message because chunking
 * ({@code activiti.cloud.runtime-bundle.events-properties.chunk-size-in-bytes-close-listener})
 * defaults to 0 = disabled. Every event holds a strong reference to the deserialized variable
 * value, and a non-{@code ephemeral} variable ships its full value in every VariableCreated /
 * VariableUpdated event.
 *
 * <p>Three process definitions separate the two effects:
 * <ul>
 *   <li>{@code miSubprocessLargeVariableSync} - no {@code activiti:async}, so every instance's
 *       events land in <em>one</em> CommandContext and therefore <em>one</em> message.</li>
 *   <li>{@code miSubprocessLargeVariableAsync} - {@code activiti:async="true"} on the subprocess,
 *       so each instance gets its own CommandContext and the list is flushed and released per
 *       branch.</li>
 *   <li>{@code miSubprocessLargeVariableNarrow} - synchronous like the first, but without
 *       {@code activiti:elementVariable} and with the user task input mapping narrowed to
 *       {@code ${batchState.documents[index]}}, which removes one VariableCreated event per
 *       instance and keeps the large value off the tasks.</li>
 * </ul>
 *
 * <p>The spy on {@code closed(CommandContext)} records, for every flush, how many events the
 * aggregator had accumulated and how many bytes they serialise to. Set a breakpoint in
 * {@code BaseCommandContextEventsAggregator.add} or in {@link #recordEventBatches()} below to
 * watch the list grow.
 */
@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(
    classes = { ServicesAuditITConfiguration.class },
    initializers = { KeycloakContainerApplicationInitializer.class }
)
@Import(TestChannelBinderConfiguration.class)
@DirtiesContext
public class MultiInstanceLargeVariableAuditIT {

    private static final String SYNC_WIDE = "miSubprocessLargeVariableSync";
    private static final String ASYNC_WIDE = "miSubprocessLargeVariableAsync";
    private static final String SYNC_NARROW = "miSubprocessLargeVariableNarrow";

    private static final String BATCH_STATE = "batchState";

    /** Overridable so profiling runs need no edit: {@code -Dmi.documents=40 -Dmi.documentSizeKb=512}. */
    private static final int DOCUMENTS = Integer.getInteger("mi.documents", 50);

    private static final int DOCUMENT_SIZE_KB = Integer.getInteger("mi.documentSizeKb", 12);

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoSpyBean
    private MessageProducerCommandContextCloseListener closeListener;

    private final List<EventBatch> batches = new CopyOnWriteArrayList<>();

    /** One aggregator flush: the events one CommandContext had accumulated, and their wire size. */
    private record EventBatch(int eventCount, int serializedBytes, Map<String, Long> countsByType) {}

    private record Stats(
        String processDefinitionKey,
        int flushes,
        int maxEvents,
        int maxBytes,
        int totalBytes,
        long allocatedBytes,
        Map<String, Long> countsByType,
        List<EventBatch> batchDetail
    ) {
        @Override
        public String toString() {
            return "%-34s flushes=%-4d maxEventsPerFlush=%-5d maxBytesPerFlush=%-10d totalBytes=%-10d allocatedMB=%d".formatted(
                    processDefinitionKey,
                    flushes,
                    maxEvents,
                    maxBytes,
                    totalBytes,
                    allocatedBytes / (1024 * 1024)
                );
        }
    }

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        // application-test.properties disables the async executor; the async variant needs it.
        registry.add("spring.activiti.asyncExecutorActivate", () -> true);
        // Isolate the schema so other IT classes in this module do not share state.
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:mi-large-variable-audit");
    }

    @BeforeEach
    public void recordEventBatches() {
        batches.clear();
        doAnswer(invocation -> {
                CommandContext commandContext = invocation.getArgument(0);
                List<CloudRuntimeEvent<?, ?>> events = commandContext.getGenericAttribute(
                    MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS
                );
                if (events != null && !events.isEmpty()) {
                    Map<String, Long> countsByType = events
                        .stream()
                        .collect(
                            Collectors.groupingBy(
                                event -> String.valueOf(event.getEventType()),
                                TreeMap::new,
                                Collectors.counting()
                            )
                        );
                    batches.add(
                        new EventBatch(events.size(), jsonMapper.writeValueAsBytes(events).length, countsByType)
                    );
                }
                return invocation.callRealMethod();
            })
            .when(closeListener)
            .closed(any(CommandContext.class));
    }

    @Test
    public void should_flushEventsPerBranch_when_multiInstanceSubprocessIsAsync() {
        Stats syncWide = run(SYNC_WIDE);
        Stats asyncWide = run(ASYNC_WIDE);
        Stats syncNarrow = run(SYNC_NARROW);

        // This module's logback-test.xml sets the root level to OFF, so the measurements go to
        // stdout, which failsafe captures into target/failsafe-reports/<class>-output.txt.
        System.out.printf(
            "%nProcessEngineEventsAggregator, %d documents of ~%d KB, -Xmx=%d MB:%n",
            DOCUMENTS,
            DOCUMENT_SIZE_KB,
            Runtime.getRuntime().maxMemory() / (1024 * 1024)
        );
        System.out.printf("  %s%n", syncWide);
        System.out.printf("  %s%n", asyncWide);
        System.out.printf("  %s%n", syncNarrow);
        System.out.printf("%nHeaviest flushes (which CommandContext the payload is built in):%n");
        printHeaviestFlushes(syncWide);
        printHeaviestFlushes(asyncWide);
        printHeaviestFlushes(syncNarrow);

        System.out.printf("%nEvent counts by type (all flushes):%n");
        System.out.printf("  %-34s %s%n", SYNC_WIDE, syncWide.countsByType());
        System.out.printf("  %-34s %s%n", ASYNC_WIDE, asyncWide.countsByType());
        System.out.printf("  %-34s %s%n", SYNC_NARROW, syncNarrow.countsByType());

        // Synchronous multi-instance pools every branch's events into a single flush, so the
        // largest flush carries more events than there are instances.
        assertThat(syncWide.maxEvents())
            .as("synchronous multi-instance pools all branches into one aggregator flush: %s", syncWide)
            .isGreaterThan(DOCUMENTS);

        // activiti:async on the subprocess gives each branch its own CommandContext, so the
        // aggregator is flushed and released per branch instead of accumulating.
        assertThat(asyncWide.maxEvents())
            .as("async multi-instance flushes the aggregator per branch: %s vs %s", asyncWide, syncWide)
            .isLessThan(syncWide.maxEvents());

        // Dropping elementVariable and narrowing the input mapping removes one VariableCreated
        // event per instance and keeps the large value off the tasks, shrinking the payload.
        // VariableEventFilter.shouldEmmitEvent keeps an event only when taskId != null or
        // executionId == processInstanceId. The multi-instance element variable is written by
        // setLoopVariable on the MI *child* execution, so it satisfies neither and never reaches
        // the aggregator: dropping activiti:elementVariable removes no event at all, and the byte
        // difference asserted below comes purely from the task input mapping.
        assertThat(variableEventCount(syncNarrow))
            .as("the multi-instance element variable is execution-local, so VariableEventFilter drops it")
            .isEqualTo(variableEventCount(syncWide));

        // Everything that is not a variable event is identical across the two definitions.
        // Note: the VARIABLE_CREATED/VARIABLE_UPDATED split does differ between them (19/6 vs
        // 13/12 for six instances) even though the total does not. That shift is not explained
        // here, which is why only the total is asserted.
        assertThat(nonVariableEventCounts(syncNarrow)).isEqualTo(nonVariableEventCounts(syncWide));

        assertThat(syncNarrow.maxBytes())
            .as("narrowing the mapping shrinks the largest single message: %s vs %s", syncNarrow, syncWide)
            .isLessThan(syncWide.maxBytes());
    }

    /**
     * Cumulative bytes allocated by every thread in this JVM. Unlike a heap-usage delta this is
     * not perturbed by GC timing, and it counts the Tomcat worker threads that actually run the
     * REST calls. It also counts everything else happening in the process during the window, so
     * only compare it between variants, never read it as the cost of the process alone.
     */
    private static long totalAllocatedBytes() {
        if (
            ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean &&
            bean.isThreadAllocatedMemorySupported()
        ) {
            if (!bean.isThreadAllocatedMemoryEnabled()) {
                bean.setThreadAllocatedMemoryEnabled(true);
            }
            return bean.getTotalThreadAllocatedBytes();
        }
        return 0L;
    }

    private static long variableEventCount(Stats stats) {
        return stats
            .countsByType()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().startsWith("VARIABLE_"))
            .mapToLong(Map.Entry::getValue)
            .sum();
    }

    private static Map<String, Long> nonVariableEventCounts(Stats stats) {
        return stats
            .countsByType()
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().startsWith("VARIABLE_"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
    }

    private static void printHeaviestFlushes(Stats stats) {
        System.out.printf("  %s%n", stats.processDefinitionKey());
        stats
            .batchDetail()
            .stream()
            .sorted(Comparator.comparingInt(EventBatch::serializedBytes).reversed())
            .limit(3)
            .forEach(batch ->
                System.out.printf(
                    "      events=%-5d bytes=%-10d %s%n",
                    batch.eventCount(),
                    batch.serializedBytes(),
                    batch.countsByType()
                )
            );
    }

    private Stats run(String processDefinitionKey) {
        batches.clear();
        long allocatedBefore = totalAllocatedBytes();

        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate.startProcessByKey(
            processDefinitionKey,
            Map.of(BATCH_STATE, buildBatchState()),
            null
        );

        for (CloudTask task : awaitTasks(processInstance)) {
            ObjectNode reviewed = jsonMapper.createObjectNode();
            reviewed.put("reviewed", true);
            taskRestTemplate.complete(
                task,
                TaskPayloadBuilder.complete().withTaskId(task.getId()).withVariable("document", reviewed).build()
            );
        }

        List<EventBatch> snapshot = new ArrayList<>(batches);
        Map<String, Long> countsByType = snapshot
            .stream()
            .flatMap(batch -> batch.countsByType().entrySet().stream())
            .collect(
                Collectors.groupingBy(Map.Entry::getKey, TreeMap::new, Collectors.summingLong(Map.Entry::getValue))
            );

        return new Stats(
            processDefinitionKey,
            snapshot.size(),
            snapshot.stream().mapToInt(EventBatch::eventCount).max().orElse(0),
            snapshot.stream().mapToInt(EventBatch::serializedBytes).max().orElse(0),
            snapshot.stream().mapToInt(EventBatch::serializedBytes).sum(),
            totalAllocatedBytes() - allocatedBefore,
            new LinkedHashMap<>(countsByType),
            snapshot
        );
    }

    /**
     * Builds {@code {"documents":[{...}, ...]}}. Each element is padded past the 4000 char inline
     * limit so it is stored as {@code longJson} in ACT_GE_BYTEARRAY, as in production.
     */
    private ObjectNode buildBatchState() {
        String padding = "x".repeat(DOCUMENT_SIZE_KB * 1024);

        ObjectNode batchState = jsonMapper.createObjectNode();
        ArrayNode documents = batchState.putArray("documents");
        for (int i = 0; i < DOCUMENTS; i++) {
            ObjectNode document = documents.addObject();
            document.put("id", "document-" + i);
            document.put("payload", padding);
        }
        return batchState;
    }

    private List<CloudTask> awaitTasks(ResponseEntity<CloudProcessInstance> processInstance) {
        // The async variant creates its instances through the async executor, and its jobs are
        // exclusive by default, so the tasks appear one at a time.
        await()
            .atMost(Duration.ofMinutes(2))
            .untilAsserted(() ->
                assertThat(processInstanceRestTemplate.getTasks(processInstance).getBody().getContent())
                    .hasSize(DOCUMENTS)
            );
        return new ArrayList<>(processInstanceRestTemplate.getTasks(processInstance).getBody().getContent());
    }
}
