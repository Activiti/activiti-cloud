/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.DeleteProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteProcessInstanceCmdExecutorTest {

    @InjectMocks
    private DeleteProcessInstanceCmdExecutor subject;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @Test
    public void startProcessInstanceCmdExecutorTest() {
        DeleteProcessPayload payload = ProcessPayloadBuilder.delete()
                                                            .withProcessInstanceId("def key")
                                                            .build();

        ProcessInstance fakeProcessInstance = mock(ProcessInstance.class);

        given(processAdminRuntime.delete(payload)).willReturn(fakeProcessInstance);

        assertThat(subject.getHandledType()).isEqualTo(DeleteProcessPayload.class.getName());

        subject.execute(payload);

        verify(processAdminRuntime).delete(payload);
    }
}
