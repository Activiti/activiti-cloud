package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.Result;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.ResumeProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
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
    private ProcessAdminRuntime processAdminRuntime;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void resumeProcessInstanceCmdExecutorTest() {
        ResumeProcessPayload resumeProcessPayload = new ResumeProcessPayload("x");

        assertThat(activateProcessInstanceCmdExecutor.getHandledType()).isEqualTo(ResumeProcessPayload.class.getName());

        activateProcessInstanceCmdExecutor.execute(resumeProcessPayload);

        verify(processAdminRuntime).resume(resumeProcessPayload);

        verify(commandResults).send(ArgumentMatchers.<Message<Result<ProcessInstance>>>any());
    }
}