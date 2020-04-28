package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class StartProcessInstanceCmdExecutorTest {

    @InjectMocks
    private StartProcessInstanceCmdExecutor startProcessInstanceCmdExecutor;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void startProcessInstanceCmdExecutorTest() {
        StartProcessPayload startProcessInstanceCmd = ProcessPayloadBuilder.start()
                .withProcessDefinitionKey("def key")
                .withName("name")
                .withBusinessKey("business key")
        .build();

        ProcessInstance fakeProcessInstance = mock(ProcessInstance.class);

        given(processAdminRuntime.start(startProcessInstanceCmd)).willReturn(fakeProcessInstance);

        assertThat(startProcessInstanceCmdExecutor.getHandledType()).isEqualTo(StartProcessPayload.class.getName());

        startProcessInstanceCmdExecutor.execute(startProcessInstanceCmd);

        verify(processAdminRuntime).start(startProcessInstanceCmd);

    }
}
