package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.cmd.ProcessCommands;
import org.activiti.runtime.api.cmd.impl.ResumeProcessImpl;
import org.activiti.runtime.api.cmd.result.ResumeProcessResult;
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

public class ActivateProcessInstanceCmdExecutorTest {

    @InjectMocks
    private ResumeProcessInstanceCmdExecutor activateProcessInstanceCmdExecutor;

    @Mock
    private SecurityAwareProcessInstanceService processInstanceService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void activateProcessInstanceCmdExecutorTest() {
        ResumeProcessImpl activateProcessInstanceCmd = new ResumeProcessImpl("x");

        assertThat(activateProcessInstanceCmdExecutor.getHandledType()).isEqualTo(ProcessCommands.RESUME_PROCESS.name());

        activateProcessInstanceCmdExecutor.execute(activateProcessInstanceCmd);

        verify(processInstanceService).activate(activateProcessInstanceCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<ResumeProcessResult>>any());
    }
}