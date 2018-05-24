package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.api.commands.results.CompleteTaskResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
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
        ReleaseTaskCmd releaseTaskCmd = new ReleaseTaskCmd("taskId");
        assertThat(releaseTaskCmdExecutor.getHandledType()).isEqualTo(ReleaseTaskCmd.class);

        //when
        releaseTaskCmdExecutor.execute(releaseTaskCmd);

        //then
        verify(securityAwareTaskService).releaseTask(releaseTaskCmd);
        verify(commandResults).send(ArgumentMatchers.<Message<CompleteTaskResults>>any());
    }
}