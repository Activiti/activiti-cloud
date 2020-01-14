package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.runtime.TaskAdminRuntime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ClaimTaskCmdExecutorTest {

    @InjectMocks
    private ClaimTaskCmdExecutor claimTaskCmdExecutor;

    @Mock
    private TaskAdminRuntime taskAdminRuntime;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void claimTaskCmdExecutorTest() {
        //given
        ClaimTaskPayload claimTaskPayload = new ClaimTaskPayload("taskId",
                                                                 "assignee");

        assertThat(claimTaskCmdExecutor.getHandledType()).isEqualTo(ClaimTaskPayload.class.getName());

        //when
        claimTaskCmdExecutor.execute(claimTaskPayload);

        //then
        verify(taskAdminRuntime).claim(claimTaskPayload);
    }
}