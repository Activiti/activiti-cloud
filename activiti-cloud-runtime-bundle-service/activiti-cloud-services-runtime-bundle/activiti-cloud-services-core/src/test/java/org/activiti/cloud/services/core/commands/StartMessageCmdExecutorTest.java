package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class StartMessageCmdExecutorTest {

    @InjectMocks
    private StartMessageCmdExecutor subject;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void signalProcessInstancesCmdExecutorTest() {
        StartMessagePayload payload = new StartMessagePayload("messageName",
                                                              "businessKey",
                                                              Collections.emptyMap());

        assertThat(subject.getHandledType()).isEqualTo(StartMessagePayload.class.getName());

        subject.execute(payload);

        verify(processAdminRuntime).start(payload);
    }
}
