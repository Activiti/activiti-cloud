package org.activiti.cloud.services.core.commands;

import org.activiti.api.model.shared.Result;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
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

public class SignalCmdExecutorTest {

    @InjectMocks
    private SignalCmdExecutor signalCmdExecutor;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void signalProcessInstancesCmdExecutorTest() {
        SignalPayload signalPayload = new SignalPayload("x",
                                                        null);

        assertThat(signalCmdExecutor.getHandledType()).isEqualTo(SignalPayload.class.getName());

        signalCmdExecutor.execute(signalPayload);

        verify(processAdminRuntime).signal(signalPayload);

        verify(commandResults).send(ArgumentMatchers.<Message<Result<Void>>>any());
    }
}