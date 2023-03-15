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

package org.activiti.services.connectors.channel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.Test;

public class PropagateCloudBpmnErrorCmdTest {

    @Test
    public void should_propagateErrorCode() {
        //given
        IntegrationContext integrationContext = mock(IntegrationContext.class);
        IntegrationError integrationErrorEvent = new IntegrationErrorImpl(
            new IntegrationRequestImpl(integrationContext),
            new CloudBpmnError("Error 51", "An error occurred")
        );

        PropagateCloudBpmnErrorCmd command = spy(
            new PropagateCloudBpmnErrorCmd(integrationErrorEvent, mock(DelegateExecution.class))
        );

        doNothing().when(command).propagateError(any());

        //when
        command.execute(mock(CommandContext.class));

        //then
        verify(command).propagateError("Error 51");
    }
}
