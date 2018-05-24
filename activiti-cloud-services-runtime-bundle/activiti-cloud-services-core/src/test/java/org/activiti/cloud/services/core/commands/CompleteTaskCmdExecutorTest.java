package org.activiti.cloud.services.core.commands;

import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
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
        CompleteTaskCmd completeTaskCmd = new CompleteTaskCmd("taskId",
                                                              variables);

        assertThat(completeTaskCmdExecutor.getHandledType()).isEqualTo(CompleteTaskCmd.class);

        completeTaskCmdExecutor.execute(completeTaskCmd);

        verify(securityAwareTaskService).completeTask(completeTaskCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<CompleteTaskResults>>any());
    }
}