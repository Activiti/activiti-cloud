package org.activiti.cloud.services.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.DeleteProcessPayload;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DeleteProcessInstanceCmdExecutorTest {

    @InjectMocks
    private DeleteProcessInstanceCmdExecutor subject;

    @Mock
    private ProcessAdminRuntime processAdminRuntime;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void startProcessInstanceCmdExecutorTest() {
        DeleteProcessPayload payload = ProcessPayloadBuilder.delete()
                                                            .withProcessInstanceId("def key")
                                                            .build();

        ProcessInstance fakeProcessInstance = mock(ProcessInstance.class);

        given(processAdminRuntime.delete(payload)).willReturn(fakeProcessInstance);

        assertThat(subject.getHandledType()).isEqualTo(DeleteProcessPayload.class.getName());

        subject.execute(payload);

        verify(processAdminRuntime).delete(payload);

    }
}