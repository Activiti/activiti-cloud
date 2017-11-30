package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.results.SuspendProcessInstanceResults;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SuspendProcessInstanceCmdExecutorTest {

    @InjectMocks
    private SuspendProcessInstanceCmdExecutor suspendProcessInstanceCmdExecutor;

    @Mock
    private ProcessEngineWrapper processEngine;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void suspendProcessInstanceCmdExecutorTest() {
        SuspendProcessInstanceCmd suspendProcessInstanceCmd = new SuspendProcessInstanceCmd("x");

        assertThat(suspendProcessInstanceCmdExecutor.getHandledType()).isEqualTo(SuspendProcessInstanceCmd.class);

        suspendProcessInstanceCmdExecutor.execute(suspendProcessInstanceCmd);

        verify(processEngine).suspend(suspendProcessInstanceCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<SuspendProcessInstanceResults>>any());
    }
}