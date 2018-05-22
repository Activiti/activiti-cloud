package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.results.SignalProcessInstancesResults;
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

public class SignalProcessInstancesCmdExecutorTest {

    @InjectMocks
    private SignalProcessInstancesCmdExecutor signalProcessInstancesCmdExecutor;

    @Mock
    private SecurityAwareProcessInstanceService processInstanceService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void signalProcessInstancesCmdExecutorTest() {
        SignalProcessInstancesCmd signalProcessInstancesCmd = new SignalProcessInstancesCmd("x");

        assertThat(signalProcessInstancesCmdExecutor.getHandledType()).isEqualTo(SignalProcessInstancesCmd.class);

        signalProcessInstancesCmdExecutor.execute(signalProcessInstancesCmd);

        verify(processInstanceService).signal(signalProcessInstancesCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<SignalProcessInstancesResults>>any());
    }
}