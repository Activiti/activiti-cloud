package org.activiti.cloud.services.core;

import org.activiti.cloud.services.api.commands.SetTaskVariablesCmd;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessEngineWrapperTest {

    @InjectMocks
    private ProcessEngineWrapper processEngineWrapper;

    @Mock
    private TaskService taskService;

    @Mock
    private AuthenticationWrapper authenticationWrapper;

    @Mock
    private RuntimeService runtimeService;

    @Before
    public void setUp() {
        initMocks(this);
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("anonymous");
    }

    @Test
    public void shouldSetTaskVariables() {
        processEngineWrapper.setTaskVariables(mock(SetTaskVariablesCmd.class));
        verify(taskService).setVariables(any(),
                                         any());
    }

    @Test
    public void shouldSetTaskVariablesLocal() {
        processEngineWrapper.setTaskVariablesLocal(mock(SetTaskVariablesCmd.class));
        verify(taskService).setVariablesLocal(any(),
                                              any());
    }

}
