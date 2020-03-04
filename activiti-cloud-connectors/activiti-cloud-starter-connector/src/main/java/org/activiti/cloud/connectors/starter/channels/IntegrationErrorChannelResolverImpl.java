package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.MessageChannel;

public class IntegrationErrorChannelResolverImpl implements IntegrationErrorChannelResolver {

    private final BinderAwareChannelResolver resolver;

    private final IntegrationErrorDestinationBuilder integrationErrorDestinationBuilder;

    @Autowired
    public IntegrationErrorChannelResolverImpl(BinderAwareChannelResolver resolver,
                                               IntegrationErrorDestinationBuilder integrationErrorDestinationBuilder) {
        this.resolver = resolver;
        this.integrationErrorDestinationBuilder = integrationErrorDestinationBuilder;
    }

    @Override
    public MessageChannel resolveDestination(IntegrationRequest event) {
        String destination = integrationErrorDestinationBuilder.buildDestination(event);

        return resolver.resolveDestination(destination);
    }




}
