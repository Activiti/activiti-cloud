package org.activiti.cloud.connectors.starter.channels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.MessageChannel;

public class IntegrationChannelResolverImplTest {
    
    private IntegrationChannelResolver subject;

    @Mock
    private BinderAwareChannelResolver resolver;

    private IntegrationResultDestinationBuilder builder;

    @Mock
    private ConnectorProperties connectorProperties;
    
    @Mock
    private MessageChannel messageChannel;
    
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        
        when(connectorProperties.getMqDestinationSeparator()).thenReturn(".");
        when(resolver.resolveDestination(Mockito.anyString())).thenReturn(messageChannel);
        
        builder = Mockito.spy(new IntegrationResultDestinationBuilderImpl(connectorProperties));
        
        subject = new IntegrationChannelResolverImpl(resolver, 
                                                     builder);
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
        MessageChannel resut = subject.resolveDestination(integrationRequest);
        
        // then
        assertThat(resut).isEqualTo(messageChannel);

        verify(builder).buildDestination(integrationRequest);
    }

}
