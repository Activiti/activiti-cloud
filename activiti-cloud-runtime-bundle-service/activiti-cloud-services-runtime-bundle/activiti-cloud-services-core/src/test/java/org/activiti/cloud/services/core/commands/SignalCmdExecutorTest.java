package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SignalCmdExecutorTest {

    @InjectMocks
    private SignalCmdExecutor signalCmdExecutor;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @BeforeEach
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
    }
}
