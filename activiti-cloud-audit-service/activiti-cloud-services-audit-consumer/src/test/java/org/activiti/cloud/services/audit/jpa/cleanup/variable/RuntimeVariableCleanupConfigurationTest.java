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
package org.activiti.cloud.services.audit.jpa.cleanup.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.TransientRuntimeVariableCleanup;
import org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload.PayloadBatchWriter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessCompletedEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.streams.config.AuditJPAStreamsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RuntimeVariableCleanupConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AuditJPAStreamsAutoConfiguration.class)
        .withBean(EntityManager.class, () -> mock(EntityManager.class))
        .withBean(EventsRepository.class, () -> mock(EventsRepository.class))
        .withBean(APIEventToEntityConverters.class, () -> mock(APIEventToEntityConverters.class));

    @Test
    void should_preserveExistingRetention_whenPolicyIsAbsent() {
        runner.run(context ->
            assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(RuntimeVariableCleanup.class)
                .doesNotHaveBean(PayloadBatchWriter.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "keep", "transient" })
    void should_selectCleanupByPolicy(String policy) {
        runner
            .withPropertyValues("activiti.cloud.processing-data.variables.retention-policy=" + policy)
            .run(context -> {
                assertThat(context).hasNotFailed();
                if (policy.equals("transient")) {
                    assertThat(context)
                        .hasSingleBean(RuntimeVariableCleanup.class)
                        .hasSingleBean(PayloadBatchWriter.class);
                    assertThat(context.getBean(RuntimeVariableCleanup.class)).isInstanceOf(
                        TransientRuntimeVariableCleanup.class
                    );
                } else {
                    assertThat(context)
                        .doesNotHaveBean(RuntimeVariableCleanup.class)
                        .doesNotHaveBean(PayloadBatchWriter.class);
                }
            });
    }

    @Test
    void should_allowReplacementStrategy() {
        var replacement = mock(RuntimeVariableCleanup.class);
        runner
            .withPropertyValues("activiti.cloud.processing-data.variables.retention-policy=transient")
            .withBean("replacementCleanup", RuntimeVariableCleanup.class, () -> replacement)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(RuntimeVariableCleanup.class);
                assertThat(context.getBean(RuntimeVariableCleanup.class)).isSameAs(replacement);
            });
    }

    @Test
    void should_useReplacementBatchWriter() {
        var replacement = mock(PayloadBatchWriter.class);
        runner
            .withPropertyValues("activiti.cloud.processing-data.variables.retention-policy=transient")
            .withBean("replacementWriter", PayloadBatchWriter.class, () -> replacement)
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(PayloadBatchWriter.class)
                    .hasSingleBean(RuntimeVariableCleanup.class);
                assertThat(context.getBean(PayloadBatchWriter.class)).isSameAs(replacement);
                var entityManager = context.getBean(EntityManager.class);
                TypedQuery<AuditEventEntity> query = mock(TypedQuery.class);
                given(entityManager.createQuery(anyString(), eq(AuditEventEntity.class))).willReturn(query);
                given(query.setParameter(anyString(), any())).willReturn(query);
                given(query.setMaxResults(100)).willReturn(query);
                var event = new VariableCreatedEventEntity();
                event.setId(1L);
                event.setVariableInstance(new VariableInstanceImpl<>("payload", "string", "secret", "process", null));
                given(query.getResultList()).willReturn(List.of(event));
                var terminal = new ProcessCompletedEventEntity();
                terminal.setProcessInstanceId("process");
                terminal.setAppName("app");

                context.getBean(RuntimeVariableCleanup.class).cleanup(List.of(terminal));

                then(replacement)
                    .should()
                    .update(
                        eq("update audit_event set variable_instance = ?, variable_previous_value = null where id = ?"),
                        eq(List.of(event)),
                        anyList()
                    );
                assertThat(event.isVariablePayloadEmpty()).isTrue();
            });
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 7, 250 })
    void should_applyConfiguredCleanupBatchSize(int batchSize) {
        runner
            .withPropertyValues(
                "activiti.cloud.processing-data.variables.retention-policy=transient",
                "activiti.cloud.processing-data.variables.cleanup-batch-size=" + batchSize
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                var entityManager = context.getBean(EntityManager.class);
                TypedQuery<AuditEventEntity> query = mock(TypedQuery.class);
                given(entityManager.createQuery(anyString(), eq(AuditEventEntity.class))).willReturn(query);
                given(query.setParameter(anyString(), any())).willReturn(query);
                given(query.setMaxResults(batchSize)).willReturn(query);
                given(query.getResultList()).willReturn(List.of());
                var terminal = new ProcessCompletedEventEntity();
                terminal.setProcessInstanceId("process");
                terminal.setAppName("app");

                context.getBean(RuntimeVariableCleanup.class).cleanup(List.of(terminal));

                then(query).should().setMaxResults(batchSize);
            });
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    void should_rejectNonPositiveCleanupBatchSize(int batchSize) {
        runner
            .withPropertyValues(
                "activiti.cloud.processing-data.variables.retention-policy=transient",
                "activiti.cloud.processing-data.variables.cleanup-batch-size=" + batchSize
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("pageSize must be greater than zero");
            });
    }
}
