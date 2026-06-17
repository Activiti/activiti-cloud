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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProcessVariableHistoryServiceTest {

    @InjectMocks
    private ProcessVariableHistoryService processVariableHistoryService;

    @Mock
    private ProcessVariableHistoryRepository historyRepository;

    @Test
    void should_returnPagedHistory_when_processInstanceIdProvided() {
        var processInstanceId = "proc-1";
        var pageable = PageRequest.of(0, 10);
        var entity = new ProcessVariableHistoryEntity();
        Page<ProcessVariableHistoryEntity> repoPage = new PageImpl<>(List.of(entity));
        given(
            historyRepository.findByProcessInstanceIdOrderByEventTimeAscSequenceNumberAsc(processInstanceId, pageable)
        ).willReturn(repoPage);

        var result = processVariableHistoryService.getVariableHistory(processInstanceId, pageable);

        assertThat(result).isSameAs(repoPage);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(entity);
    }

    @Test
    void should_returnEmptyPage_when_noHistoryForProcessInstance() {
        var processInstanceId = "proc-unknown";
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessVariableHistoryEntity> emptyPage = new PageImpl<>(List.of());
        given(
            historyRepository.findByProcessInstanceIdOrderByEventTimeAscSequenceNumberAsc(processInstanceId, pageable)
        ).willReturn(emptyPage);

        var result = processVariableHistoryService.getVariableHistory(processInstanceId, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
