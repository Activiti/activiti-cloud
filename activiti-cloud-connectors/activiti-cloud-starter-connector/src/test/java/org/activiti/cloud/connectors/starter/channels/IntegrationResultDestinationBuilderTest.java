package org.activiti.cloud.connectors.starter.channels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class IntegrationResultDestinationBuilderTest {
    
    @InjectMocks
    private IntegrationResultDestinationBuilderImpl subject;

    @Mock
    private ConnectorProperties connectorProperties;
    
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        
        when(connectorProperties.getMqDestinationSeparator()).thenReturn(".");
    }

    @Test
    public void shouldResolveDestination() {
        // given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        IntegrationRequestImpl integrationRequest = new IntegrationRequestImpl(integrationContext);
        integrationRequest.setServiceFullName("myApp");
        integrationRequest.setAppName("myAppName");
        integrationRequest.setAppVersion("1.0");
        integrationRequest.setServiceType("RUNTIME_BUNDLE");
        integrationRequest.setServiceVersion("1.0");

        // when
        String resut = subject.buildDestination(integrationRequest);
        
        // then
        assertThat(resut).isEqualTo("integrationResult.myApp");

    }

}
