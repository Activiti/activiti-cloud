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
package org.activiti.cloud.starter.tests.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.activiti.cloud.services.job.executor.JobMessageHandler;
import org.activiti.cloud.services.job.executor.JobMessageHandlerFactory;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starter.rb.configuration.ActivitiRuntimeBundle;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.TestSocketUtils;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class BatchStateJsonJobExecutorIT {

    private static final String PROCESS_KEY = "batchStateJsonAsyncServiceTask";
    private static final String BATCH_STATE = "batchState";
    private static final String FAIL_AFTER_MATERIALIZATION = "failAfterMaterialization";
    private static final int SMALL_BATCH_STATE_BYTES = 256 * 1024;
    private static final int DEFAULT_LARGE_BATCH_STATE_BYTES = 2 * 1024 * 1024;
    private static final int RETRY_COUNT = 3;

    private static ConfigurableApplicationContext rbCtx;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @SpringBootApplication
    @ActivitiRuntimeBundle
    static class RbApplication {

        @Bean
        public JobMessageHandlerFactory jobMessageHandlerFactory() {
            return configuration -> spy(new JobMessageHandler(configuration));
        }

        @Bean
        public BatchStateReaderDelegate batchStateReaderDelegate(ObjectMapper objectMapper) {
            return new BatchStateReaderDelegate(objectMapper);
        }
    }

    @BeforeAll
    static void setUp() {
        new KeycloakContainerApplicationInitializer().initialize();
        String[] rabbitMqProperties = RabbitMQContainerApplicationInitializer.initialize();
        String datasourcePasswordProperty = "spring.datasource." + "pass" + "word=" + postgres.getPassword();
        String asyncExecutorProperty = "spring.activiti.asyncExecutorActivate=true";
        String[] datasource = new String[] {
            "spring.datasource.url=" + postgres.getJdbcUrl(),
            "spring.datasource.username=" + postgres.getUsername(),
            datasourcePasswordProperty,
            asyncExecutorProperty,
        };

        rbCtx =
            TestPropertyValues.of(rabbitMqProperties)
                .and(datasource)
                .applyToSystemProperties(() ->
                    new SpringApplicationBuilder(RbApplication.class)
                        .properties("server.port=" + TestSocketUtils.findAvailableTcpPort())
                        .run()
                );
    }

    @AfterAll
    static void tearDown() {
        rbCtx.close();
    }

    @BeforeEach
    void resetState() {
        reset(jobMessageHandler());
        delegate().reset();
    }

    @Test
    void shouldPersistBatchStateInActGeByteArrayAndCompleteForSmallerPayload() {
        String batchStateJson = buildBatchStateJson(SMALL_BATCH_STATE_BYTES);
        ProcessInstance processInstance = runtimeService()
            .startProcessInstanceByKey(PROCESS_KEY, variables(batchStateJson, false));

        await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            Task task = taskService().createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();

            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("Review batch state");
        });

        String byteArrayId = findBatchStateByteArrayId(processInstance.getProcessInstanceId());
        Integer persistedBytes = jdbcTemplate()
            .queryForObject(
                "select octet_length(BYTES_) from ACT_GE_BYTEARRAY where ID_ = ?",
                Integer.class,
                byteArrayId
            );

        assertThat(byteArrayId).isNotBlank();
        assertThat(persistedBytes).isGreaterThanOrEqualTo(batchStateJson.getBytes(StandardCharsets.UTF_8).length);
        assertThat(delegate().getInvocationCount()).isEqualTo(1);
        assertThat(delegate().getLargestMaterializedBatchStateBytes())
            .isGreaterThanOrEqualTo(batchStateJson.getBytes(StandardCharsets.UTF_8).length);
        verify(jobMessageHandler(), atLeastOnce()).handleMessage(any());

        taskService()
            .complete(taskService().createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult().getId());

        await().atMost(Duration.ofMinutes(1)).untilAsserted(() ->
            assertThat(runtimeService().createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero()
        );
    }

    @Test
    void shouldRetryAfterMaterializingLargeBatchStateJson() {
        String batchStateJson = buildBatchStateJson(Integer.getInteger("batch.state.reproducer.large-json-bytes", DEFAULT_LARGE_BATCH_STATE_BYTES));
        ProcessInstance processInstance = runtimeService()
            .startProcessInstanceByKey(PROCESS_KEY, variables(batchStateJson, true));

        await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            assertThat(
                managementService().createDeadLetterJobQuery().processInstanceId(processInstance.getProcessInstanceId()).count()
            )
                .isEqualTo(1);
            assertThat(delegate().getInvocationCount()).isEqualTo(RETRY_COUNT);
        });

        String byteArrayId = findBatchStateByteArrayId(processInstance.getProcessInstanceId());
        Integer persistedBytes = jdbcTemplate()
            .queryForObject(
                "select octet_length(BYTES_) from ACT_GE_BYTEARRAY where ID_ = ?",
                Integer.class,
                byteArrayId
            );
        Job deadLetterJob = managementService()
            .createDeadLetterJobQuery()
            .processInstanceId(processInstance.getProcessInstanceId())
            .withException()
            .singleResult();

        assertThat(byteArrayId).isNotBlank();
        assertThat(persistedBytes).isGreaterThanOrEqualTo(batchStateJson.getBytes(StandardCharsets.UTF_8).length);
        assertThat(delegate().getLargestMaterializedBatchStateBytes())
            .isGreaterThanOrEqualTo(batchStateJson.getBytes(StandardCharsets.UTF_8).length);
        assertThat(deadLetterJob.getExceptionMessage()).contains("Simulated failure after materializing batchState JSON");
        verify(jobMessageHandler(), times(RETRY_COUNT)).handleMessage(any());
    }

    private static Map<String, Object> variables(String batchStateJson, boolean failAfterMaterialization) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(BATCH_STATE, batchStateJson);
        variables.put(FAIL_AFTER_MATERIALIZATION, failAfterMaterialization);
        return variables;
    }

    private JobMessageHandler jobMessageHandler() {
        return rbCtx.getBean(JobMessageHandler.class);
    }

    private BatchStateReaderDelegate delegate() {
        return rbCtx.getBean(BatchStateReaderDelegate.class);
    }

    private RuntimeService runtimeService() {
        return rbCtx.getBean(RuntimeService.class);
    }

    private TaskService taskService() {
        return rbCtx.getBean(TaskService.class);
    }

    private ManagementService managementService() {
        return rbCtx.getBean(ManagementService.class);
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(rbCtx.getBean(DataSource.class));
    }

    private String findBatchStateByteArrayId(String processInstanceId) {
        return jdbcTemplate()
            .queryForObject(
                "select BYTEARRAY_ID_ from ACT_RU_VARIABLE where PROC_INST_ID_ = ? and NAME_ = ?",
                String.class,
                processInstanceId,
                BATCH_STATE
            );
    }

    private static String buildBatchStateJson(int targetBytes) {
        String repeatedPayload = "x".repeat(1024);
        StringBuilder builder = new StringBuilder(targetBytes + 256);
        builder.append("{\"items\":[");

        int index = 0;
        while (builder.length() < targetBytes) {
            if (index > 0) {
                builder.append(',');
            }
            builder
                .append("{\"id\":")
                .append(index)
                .append(",\"state\":\"READY\",\"payload\":\"")
                .append(repeatedPayload)
                .append("\"}");
            index++;
        }

        builder.append("]}");
        return builder.toString();
    }

    static class BatchStateReaderDelegate implements JavaDelegate {

        private final ObjectMapper objectMapper;
        private final AtomicInteger invocationCount = new AtomicInteger();
        private final AtomicInteger largestMaterializedBatchStateBytes = new AtomicInteger();

        BatchStateReaderDelegate(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void execute(DelegateExecution execution) throws Exception {
            invocationCount.incrementAndGet();

            String batchStateJson = (String) execution.getVariable(BATCH_STATE);
            largestMaterializedBatchStateBytes.accumulateAndGet(
                batchStateJson.getBytes(StandardCharsets.UTF_8).length,
                Math::max
            );

            JsonNode batchState = objectMapper.readTree(batchStateJson);
            execution.setVariableLocal("batchStateItemCount", batchState.path("items").size());

            if (Boolean.TRUE.equals(execution.getVariable(FAIL_AFTER_MATERIALIZATION))) {
                throw new IllegalStateException("Simulated failure after materializing batchState JSON");
            }
        }

        void reset() {
            invocationCount.set(0);
            largestMaterializedBatchStateBytes.set(0);
        }

        int getInvocationCount() {
            return invocationCount.get();
        }

        int getLargestMaterializedBatchStateBytes() {
            return largestMaterializedBatchStateBytes.get();
        }
    }
}
