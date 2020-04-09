package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.payloads.SuspendProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SuspendProcessInstanceCmdExecutorTest {

    @InjectMocks
    private SuspendProcessInstanceCmdExecutor suspendProcessInstanceCmdExecutor;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void suspendProcessInstanceCmdExecutorTest() {
        //given
        SuspendProcessPayload suspendProcessInstanceCmd = new SuspendProcessPayload("x");
        assertThat(suspendProcessInstanceCmdExecutor.getHandledType()).isEqualTo(SuspendProcessPayload.class.getName());

        //when
        suspendProcessInstanceCmdExecutor.execute(suspendProcessInstanceCmd);

        //then
        verify(processAdminRuntime).suspend(suspendProcessInstanceCmd);
    }
}
