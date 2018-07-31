package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.Result;
import org.activiti.runtime.api.model.payloads.SignalPayload;
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

public class SignalCmdExecutorTest {

    @InjectMocks
    private SignalCmdExecutor signalCmdExecutor;

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
        SignalPayload signalPayload = new SignalPayload("x",
                                                        null);

        assertThat(signalCmdExecutor.getHandledType()).isEqualTo(SignalPayload.class.getName());

        signalCmdExecutor.execute(signalPayload);

        verify(processInstanceService).signal(signalPayload);

        verify(commandResults).send(ArgumentMatchers.<Message<Result<Void>>>any());
    }
}