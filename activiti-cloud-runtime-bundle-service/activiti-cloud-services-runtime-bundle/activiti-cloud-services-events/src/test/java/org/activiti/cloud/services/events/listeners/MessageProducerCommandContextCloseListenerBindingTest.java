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
package org.activiti.cloud.services.events.listeners;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.util.List;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({ TestChannelBinderConfiguration.class })
@ActiveProfiles("binding")
class MessageProducerCommandContextCloseListenerBindingTest {

    @SpringBootApplication
    static class Application {
    }
    
    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private MessageProducerCommandContextCloseListener messageProducerCommandContextCloseListener;

    @MockBean
    private UserDetailsService userDetailsService;

    @Mock
    private CommandContext commandContext;

    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Test
    public void shouldHaveChannelBindingsSetForAuditProducer() {
        //given
        String destination = "engineEvents";
        ProcessInstance processInstance = new ProcessInstanceImpl();
        processEngineEventsAggregator = spy(new ProcessEngineEventsAggregator(messageProducerCommandContextCloseListener));

        //when
        when(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS)).thenReturn(
                List.of(new CloudProcessCreatedEventImpl(processInstance)));
        when(processEngineEventsAggregator.getCurrentCommandContext()).thenReturn(commandContext);

        messageProducerCommandContextCloseListener.closed(commandContext);

        //then
        Message<byte[]> received = outputDestination.receive(0l, destination);
        assertNotNull(received);
    }
}