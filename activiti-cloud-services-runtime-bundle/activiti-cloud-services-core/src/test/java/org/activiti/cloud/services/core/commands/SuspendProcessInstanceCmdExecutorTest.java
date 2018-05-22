package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.results.SuspendProcessInstanceResults;
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

public class SuspendProcessInstanceCmdExecutorTest {

    @InjectMocks
    private SuspendProcessInstanceCmdExecutor suspendProcessInstanceCmdExecutor;

    @Mock
    private SecurityAwareProcessInstanceService processInstanceService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void suspendProcessInstanceCmdExecutorTest() {
        //given
        SuspendProcessInstanceCmd suspendProcessInstanceCmd = new SuspendProcessInstanceCmd("x");
        assertThat(suspendProcessInstanceCmdExecutor.getHandledType()).isEqualTo(SuspendProcessInstanceCmd.class);

        //when
        suspendProcessInstanceCmdExecutor.execute(suspendProcessInstanceCmd);

        //then
        verify(processInstanceService).suspend(suspendProcessInstanceCmd);
        verify(commandResults).send(ArgumentMatchers.<Message<SuspendProcessInstanceResults>>any());
    }
}