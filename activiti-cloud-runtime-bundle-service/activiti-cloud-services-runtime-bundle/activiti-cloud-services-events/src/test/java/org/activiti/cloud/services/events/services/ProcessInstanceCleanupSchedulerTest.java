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
package org.activiti.cloud.services.events.services;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import org.activiti.cloud.services.core.conf.ProcessCleanupProperties;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceCleanupSchedulerTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private CloudProcessDeletedService cloudProcessDeletedService;

    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @Mock
    private HistoricProcessInstance processInstance1;

    @Mock
    private HistoricProcessInstance processInstance2;

    private ProcessInstanceCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        ProcessCleanupProperties properties = new ProcessCleanupProperties();
        properties.setGracePeriod(Duration.ofMinutes(5));
        properties.setCleanupInterval(Duration.ofMinutes(1));
        properties.setBatchSize(100);

        scheduler = new ProcessInstanceCleanupScheduler(
            historyService,
            cloudProcessDeletedService,
            properties
        );

        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.finished()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.finishedBefore(any(Date.class))).thenReturn(historicProcessInstanceQuery);
    }

    @Test
    void shouldDeleteOldProcessInstances() {
        when(processInstance1.getId()).thenReturn("proc-1");
        when(processInstance2.getId()).thenReturn("proc-2");
        when(historicProcessInstanceQuery.listPage(0, 100)).thenReturn(List.of(processInstance1, processInstance2));

        scheduler.cleanupOldProcessInstances();

        verify(historicProcessInstanceQuery).finishedBefore(any(Date.class));
        verify(historicProcessInstanceQuery).listPage(0, 100);
        verify(cloudProcessDeletedService).delete("proc-1");
        verify(cloudProcessDeletedService).delete("proc-2");
    }

    @Test
    void shouldHandleEmptyResult() {
        when(historicProcessInstanceQuery.listPage(0, 100)).thenReturn(List.of());

        scheduler.cleanupOldProcessInstances();

        verify(cloudProcessDeletedService, never()).delete(any());
    }

    @Test
    void shouldContinueOnError() {
        when(processInstance1.getId()).thenReturn("proc-1");
        when(processInstance2.getId()).thenReturn("proc-2");
        when(historicProcessInstanceQuery.listPage(anyInt(), anyInt())).thenReturn(List.of(processInstance1, processInstance2));
        doThrow(new RuntimeException("Delete failed")).when(cloudProcessDeletedService).delete("proc-1");

        scheduler.cleanupOldProcessInstances();

        verify(cloudProcessDeletedService).delete("proc-1");
        verify(cloudProcessDeletedService).delete("proc-2");
    }

    @Test
    void shouldHandleQueryError() {
        when(historicProcessInstanceQuery.listPage(anyInt(), anyInt())).thenThrow(new RuntimeException("Query failed"));

        scheduler.cleanupOldProcessInstances();

        verify(cloudProcessDeletedService, never()).delete(any());
    }
}
