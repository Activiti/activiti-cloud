package org.activiti.cloud.services.core.commands;

import org.activiti.runtime.api.ProcessAdminRuntime;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class SuspendProcessInstanceCmdExecutorTest {

    @InjectMocks
    private SuspendProcessInstanceCmdExecutor suspendProcessInstanceCmdExecutor;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void suspendProcessInstanceCmdExecutorTest() {
        //given
        SuspendProcessPayload suspendProcessInstanceCmd = new SuspendProcessPayload("x");
        assertThat(suspendProcessInstanceCmdExecutor.getHandledType()).isEqualTo(SuspendProcessPayload.class.getName());

        //when
        suspendProcessInstanceCmdExecutor.execute(suspendProcessInstanceCmd);

        //then
        verify(processAdminRuntime).suspend(suspendProcessInstanceCmd);
        verify(commandResults).send(ArgumentMatchers.<Message<Result<ProcessInstance>>>any());
    }
}