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
package org.activiti.cloud.starter.query.consumer.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.activiti.cloud.services.test.TestProducerAutoConfiguration;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = QueryConsumerTestApplication.class,
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.main.web-application-type=none",
    }
)
@EnableTestBinder
@Import(TestProducerAutoConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
class ProcessVariableHistoryPersistenceIT {

    @Autowired
    private MyProducer producer;

    @Autowired
    private ProcessVariableHistoryRepository historyRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @AfterEach
    void tearDown() {
        historyRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("java:S5961")
    void should_persistHistoryEntries_when_createUpdateDeleteLifecycle() {
        String processInstanceId = "proc-it-001";
        var processInstance = new ProcessInstanceEntity();
        processInstance.setId(processInstanceId);
        processInstance.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        processInstanceRepository.save(processInstance);

        long baseTimestamp = System.currentTimeMillis();

        producer.send(
            new CloudVariableCreatedEventImpl(
                "e1",
                baseTimestamp,
                new VariableInstanceImpl<>("myVar", "string", "initial", processInstanceId, null)
            )
        );

        producer.send(
            new CloudVariableUpdatedEventImpl<>(
                "e2",
                baseTimestamp + 1000,
                new VariableInstanceImpl<>("myVar", "string", "second", processInstanceId, null),
                "initial"
            )
        );

        producer.send(
            new CloudVariableUpdatedEventImpl<>(
                "e3",
                baseTimestamp + 2000,
                new VariableInstanceImpl<>("myVar", "string", "third", processInstanceId, null),
                "second"
            )
        );

        producer.send(
            new CloudVariableDeletedEventImpl(
                "e4",
                baseTimestamp + 3000,
                new VariableInstanceImpl<>("myVar", "string", null, processInstanceId, null)
            )
        );

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                assertThat(
                    historyRepository.findByProcessInstanceIdAndVariableNameOrderByEventTimeAscSequenceNumberAsc(
                        processInstanceId,
                        "myVar"
                    )
                ).hasSize(4)
            );

        List<ProcessVariableHistoryEntity> history = historyRepository.findByProcessInstanceIdAndVariableNameOrderByEventTimeAscSequenceNumberAsc(
            processInstanceId,
            "myVar"
        );

        assertThat(history).hasSize(4);

        ProcessVariableHistoryEntity entry0 = history.getFirst();
        assertThat(entry0.getVariableName()).isEqualTo("myVar");
        assertThat(entry0.getType()).isEqualTo("string");
        assertThat((String) entry0.getValue()).isEqualTo("initial");
        assertThat(entry0.isDeleted()).isFalse();
        assertThat(entry0.getEventTime().getTime()).isEqualTo(baseTimestamp);
        assertThat(entry0.getRecordCreateTime()).isNotNull();
        assertThat(entry0.getMessageId()).isNotNull();
        assertThat(entry0.getSequenceNumber()).isZero();

        ProcessVariableHistoryEntity entry1 = history.get(1);
        assertThat((String) entry1.getValue()).isEqualTo("second");
        assertThat(entry1.isDeleted()).isFalse();
        assertThat(entry1.getEventTime().getTime()).isEqualTo(baseTimestamp + 1000);
        assertThat(entry1.getRecordCreateTime()).isNotNull();
        assertThat(entry1.getMessageId()).isNotNull();
        assertThat(entry1.getSequenceNumber()).isZero();

        ProcessVariableHistoryEntity entry2 = history.get(2);
        assertThat((String) entry2.getValue()).isEqualTo("third");
        assertThat(entry2.isDeleted()).isFalse();
        assertThat(entry2.getEventTime().getTime()).isEqualTo(baseTimestamp + 2000);
        assertThat(entry2.getRecordCreateTime()).isNotNull();
        assertThat(entry2.getMessageId()).isNotNull();
        assertThat(entry2.getSequenceNumber()).isZero();

        ProcessVariableHistoryEntity entry3 = history.get(3);
        assertThat((Object) entry3.getValue()).isNull();
        assertThat(entry3.isDeleted()).isTrue();
        assertThat(entry3.getEventTime().getTime()).isEqualTo(baseTimestamp + 3000);
        assertThat(entry3.getRecordCreateTime()).isNotNull();
        assertThat(entry3.getMessageId()).isNotNull();
        assertThat(entry3.getSequenceNumber()).isZero();

        assertThat(history)
            .extracting(ProcessVariableHistoryEntity::getMessageId)
            .doesNotContainNull()
            .doesNotHaveDuplicates();
    }
}
