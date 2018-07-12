package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.cmd.TaskCommands;
import org.activiti.runtime.api.cmd.impl.ClaimTaskImpl;
import org.activiti.runtime.api.cmd.result.ClaimTaskResult;
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

public class ClaimTaskCmdExecutorTest {

    @InjectMocks
    private ClaimTaskCmdExecutor claimTaskCmdExecutor;

    @Mock
    private SecurityAwareTaskService securityAwareTaskService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void claimTaskCmdExecutorTest() {
        //given
        ClaimTaskImpl claimTaskCmd = new ClaimTaskImpl("taskId",
                                                       "assignee");

        assertThat(claimTaskCmdExecutor.getHandledType()).isEqualTo(TaskCommands.CLAIM_TASK.name());

        //when
        claimTaskCmdExecutor.execute(claimTaskCmd);

        //then
        verify(securityAwareTaskService).claimTask(claimTaskCmd);
        verify(commandResults).send(ArgumentMatchers.<Message<ClaimTaskResult>>any());
    }
}