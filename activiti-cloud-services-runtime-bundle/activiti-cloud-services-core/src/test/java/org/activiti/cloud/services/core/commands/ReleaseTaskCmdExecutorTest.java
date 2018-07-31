package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.payloads.ReleaseTaskPayload;
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
        ReleaseTaskPayload releaseTaskPayload = new ReleaseTaskPayload("taskId");
        assertThat(releaseTaskCmdExecutor.getHandledType()).isEqualTo(ReleaseTaskPayload.class.getName());

        //when
        releaseTaskCmdExecutor.execute(releaseTaskPayload);

        //then
        verify(securityAwareTaskService).releaseTask(releaseTaskPayload);
        verify(commandResults).send(ArgumentMatchers.<Message<Result<Task>>>any());
    }
}