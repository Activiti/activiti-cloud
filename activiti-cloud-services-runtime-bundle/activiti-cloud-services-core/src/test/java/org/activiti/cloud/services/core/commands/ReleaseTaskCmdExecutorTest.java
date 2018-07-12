package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.cmd.TaskCommands;
import org.activiti.runtime.api.cmd.impl.ReleaseTaskImpl;
import org.activiti.runtime.api.cmd.result.CompleteTaskResult;
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
    private SecurityAwareTaskService securityAwareTaskService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void releaseTaskCmdExecutorTest() {
        //given
        ReleaseTaskImpl releaseTaskCmd = new ReleaseTaskImpl("taskId");
        assertThat(releaseTaskCmdExecutor.getHandledType()).isEqualTo(TaskCommands.RELEASE_TASK.name());

        //when
        releaseTaskCmdExecutor.execute(releaseTaskCmd);

        //then
        verify(securityAwareTaskService).releaseTask(releaseTaskCmd);
        verify(commandResults).send(ArgumentMatchers.<Message<CompleteTaskResult>>any());
    }
}