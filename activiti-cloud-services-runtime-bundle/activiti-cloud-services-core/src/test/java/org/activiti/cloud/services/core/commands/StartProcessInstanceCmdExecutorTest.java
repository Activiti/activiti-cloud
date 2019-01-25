package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.model.shared.Result;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class StartProcessInstanceCmdExecutorTest {

    @InjectMocks
    private StartProcessInstanceCmdExecutor startProcessInstanceCmdExecutor;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void startProcessInstanceCmdExecutorTest() {
        StartProcessPayload startProcessInstanceCmd = ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("def key")
                .withName("name")
                .withBusinessKey("business key")
        .build();

        ProcessInstance fakeProcessInstance = mock(ProcessInstance.class);

        given(processAdminRuntime.start(any())).willReturn(fakeProcessInstance);

        assertThat(startProcessInstanceCmdExecutor.getHandledType()).isEqualTo(StartProcessPayload.class.getName());

        startProcessInstanceCmdExecutor.execute(startProcessInstanceCmd);

        verify(processAdminRuntime).start(startProcessInstanceCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<Result<ProcessInstance>>>any());
    }
}