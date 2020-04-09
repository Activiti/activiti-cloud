package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ReceiveMessageCmdExecutorTest {

    @InjectMocks
    private ReceiveMessageCmdExecutor subject;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void signalProcessInstancesCmdExecutorTest() {
        ReceiveMessagePayload payload = new ReceiveMessagePayload("messageName",
                                                                  "correlationKey",
                                                                  Collections.emptyMap());

        assertThat(subject.getHandledType()).isEqualTo(ReceiveMessagePayload.class.getName());

        subject.execute(payload);

        verify(processAdminRuntime).receive(payload);
    }
}
