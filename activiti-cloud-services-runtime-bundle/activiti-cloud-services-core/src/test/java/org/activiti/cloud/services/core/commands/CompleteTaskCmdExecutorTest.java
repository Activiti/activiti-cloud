package org.activiti.cloud.services.core.commands;

import java.util.HashMap;
import java.util.Map;

import org.activiti.api.model.shared.Result;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
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

public class CompleteTaskCmdExecutorTest {

    @InjectMocks
    private CompleteTaskCmdExecutor completeTaskCmdExecutor;

    @Mock
    private TaskAdminRuntime taskAdminRuntime;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void completeTaskCmdExecutorTest() {
        Map<String, Object> variables = new HashMap<>();
        CompleteTaskPayload completeTaskPayload = new CompleteTaskPayload("taskId",
                                                                          variables);

        assertThat(completeTaskCmdExecutor.getHandledType()).isEqualTo(CompleteTaskPayload.class.getName());

        completeTaskCmdExecutor.execute(completeTaskPayload);

        verify(taskAdminRuntime).complete(completeTaskPayload);

        verify(commandResults).send(ArgumentMatchers.<Message<Result<Task>>>any());
    }
}