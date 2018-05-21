package org.activiti.cloud.services.core.commands;

import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.results.StartProcessInstanceResults;
import org.activiti.cloud.services.core.pageable.SecurityAwareProcessInstanceService;
import org.activiti.runtime.api.model.FluentProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class StartProcessInstanceCmdExecutorTest {

    @InjectMocks
    private StartProcessInstanceCmdExecutor startProcessInstanceCmdExecutor;

    @Mock
    private SecurityAwareProcessInstanceService securityAwareProcessInstanceService;

    @Mock
    private MessageChannel commandResults;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void startProcessInstanceCmdExecutorTest() {
        StartProcessInstanceCmd startProcessInstanceCmd = new StartProcessInstanceCmd("x");

        FluentProcessInstance fakeProcessInstance = mock(FluentProcessInstance.class);

        given(securityAwareProcessInstanceService.startProcess(any())).willReturn(fakeProcessInstance);

        assertThat(startProcessInstanceCmdExecutor.getHandledType()).isEqualTo(StartProcessInstanceCmd.class);

        startProcessInstanceCmdExecutor.execute(startProcessInstanceCmd);

        verify(securityAwareProcessInstanceService).startProcess(startProcessInstanceCmd);

        verify(commandResults).send(ArgumentMatchers.<Message<StartProcessInstanceResults>>any());
    }
}