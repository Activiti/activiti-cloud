package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.results.ActivateProcessInstanceResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
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
    private ActivateProcessInstanceCmdExecutor activateProcessInstanceCmdExecutor;

    @Mock
    private SecurityAwareProcessInstanceService processInstanceService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void activateProcessInstanceCmdExecutorTest() {
        ActivateProcessInstanceCmd activateProcessInstanceCmd = new ActivateProcessInstanceCmd("x");

        assertThat(activateProcessInstanceCmdExecutor.getHandledType()).isEqualTo(ActivateProcessInstanceCmd.class);

        activateProcessInstanceCmdExecutor.execute(activateProcessInstanceCmd);

        verify(processInstanceService).activate(activateProcessInstanceCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<ActivateProcessInstanceResults>>any());
    }
}