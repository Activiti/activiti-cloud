package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.MessageChannel;

public class IntegrationChannelResolverImpl implements IntegrationChannelResolver {

    @Value("${ACT_INT_RES_CONSUMER:}")
    private String resultDestinationOverride;

    private final BinderAwareChannelResolver resolver;

    private final IntegrationResultDestinationBuilder integrationResultDestinationBuilder;

    @Autowired
    public IntegrationChannelResolverImpl(BinderAwareChannelResolver resolver, 
                                          IntegrationResultDestinationBuilder integrationResultDestinationBuilder) {
        this.resolver = resolver;
        this.integrationResultDestinationBuilder = integrationResultDestinationBuilder;
    }
    
    @Override
    public MessageChannel resolveDestination(IntegrationRequest event) {
        String destination = integrationResultDestinationBuilder.buildDestination(event);
                
        return resolver.resolveDestination(destination);
    }
    
    
    

}
