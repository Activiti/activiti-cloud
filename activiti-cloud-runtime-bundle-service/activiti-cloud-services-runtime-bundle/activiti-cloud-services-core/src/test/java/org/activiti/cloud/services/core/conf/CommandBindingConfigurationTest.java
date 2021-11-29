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
package org.activiti.cloud.services.core.conf;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import java.io.IOException;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.results.ProcessInstanceResult;
import org.activiti.cloud.services.core.Application;
import org.activiti.cloud.services.core.commands.StartProcessInstanceCmdExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = { Application.class })
@Import({ TestChannelBinderConfiguration.class })
@DirtiesContext
@ActiveProfiles("binding")
public class CommandBindingConfigurationTest {

    @Autowired
    private InputDestination inputDestination;
    @Autowired
    private OutputDestination outputDestination;

    @MockBean
    StartProcessInstanceCmdExecutor startProcessInstanceCmdExecutor;
    
    private static final String COMMAND_RESULTS_BINDING = "commandProcessor-out-0";

    @Test
    public void shouldHaveChannelBindingsSetForCommandProcessor() throws IOException {
        //given
        StartProcessPayload startProcessInstanceCmd = ProcessPayloadBuilder.start().withProcessDefinitionId("id").build();
        when(startProcessInstanceCmdExecutor.getHandledType()).thenReturn(startProcessInstanceCmd.getClass().getName());
        when(startProcessInstanceCmdExecutor.execute(startProcessInstanceCmd)).thenReturn(
                new ProcessInstanceResult(startProcessInstanceCmd, null));

        //when
        Message<StartProcessPayload> message = new GenericMessage<>(startProcessInstanceCmd);
        inputDestination.send(message);
        Message<byte[]> output = outputDestination.receive(0L, COMMAND_RESULTS_BINDING);

        //then
        assertThat(output).as("Functional binding failed to receive a message").isNotNull();
    }
}