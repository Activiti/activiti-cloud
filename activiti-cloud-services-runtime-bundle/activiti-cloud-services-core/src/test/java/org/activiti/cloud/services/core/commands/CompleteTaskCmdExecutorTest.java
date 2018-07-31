package org.activiti.cloud.services.core.commands;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.services.core.pageable.SecurityAwareTaskService;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.payloads.CompleteTaskPayload;
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

public class CompleteTaskCmdExecutorTest {

    @InjectMocks
    private CompleteTaskCmdExecutor completeTaskCmdExecutor;

    @Mock
    private SecurityAwareTaskService securityAwareTaskService;

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

        verify(securityAwareTaskService).completeTask(completeTaskPayload);

        verify(commandResults).send(ArgumentMatchers.<Message<Result<Task>>>any());
    }
}