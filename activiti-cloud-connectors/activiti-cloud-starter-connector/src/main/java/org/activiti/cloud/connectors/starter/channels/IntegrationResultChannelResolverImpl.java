package org.activiti.cloud.connectors.starter.channels;

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.messaging.MessageChannel;

public class IntegrationResultChannelResolverImpl implements IntegrationResultChannelResolver {

    private final BinderAwareChannelResolver resolver;

    private final IntegrationResultDestinationBuilder integrationResultDestinationBuilder;

    public IntegrationResultChannelResolverImpl(BinderAwareChannelResolver resolver,
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
