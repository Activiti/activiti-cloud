/*
 * Copyright 2010-2020 Alfresco Software, Ltd.
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

package org.activiti.services.connectors.channel;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ExecutionQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IntegrationRequestReplayerTest {

    @Mock
    private RuntimeService runtimeService;

    @InjectMocks
    private IntegrationRequestReplayer integrationRequestReplayer;

    @Mock(answer = Answers.RETURNS_SELF)
    private ExecutionQuery executionQuery;

    @Test
    public void replay_should_throwException_when_no_executionIsFound() {
        given(runtimeService.createExecutionQuery()).willReturn(executionQuery);
        given(executionQuery.list()).willReturn(Collections.emptyList());


        assertThatExceptionOfType(ActivitiException.class)
            .isThrownBy(
            () -> integrationRequestReplayer.replay("missingExecution", "missingFlowNode"))
                .withMessageContaining("Unable to replay integration request");
    }
}
