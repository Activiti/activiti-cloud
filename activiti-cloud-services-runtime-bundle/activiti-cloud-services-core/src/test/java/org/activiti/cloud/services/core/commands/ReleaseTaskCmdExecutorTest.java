package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.Result;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.payloads.ReleaseTaskPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
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

public class ReleaseTaskCmdExecutorTest {

    @InjectMocks
    private ReleaseTaskCmdExecutor releaseTaskCmdExecutor;

    @Mock
    private TaskAdminRuntime taskAdminRuntime;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void releaseTaskCmdExecutorTest() {
        //given
        ReleaseTaskPayload releaseTaskPayload = new ReleaseTaskPayload("taskId");
        assertThat(releaseTaskCmdExecutor.getHandledType()).isEqualTo(ReleaseTaskPayload.class.getName());

        //when
        releaseTaskCmdExecutor.execute(releaseTaskPayload);

        //then
        verify(taskAdminRuntime).release(releaseTaskPayload);
        verify(commandResults).send(ArgumentMatchers.<Message<Result<Task>>>any());
    }
}