package org.activiti.cloud.services.audit.jpa.streams.config;

import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannelHandler;
import org.activiti.cloud.services.audit.api.streams.AuditConsumerChannels;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.streams.AuditConsumerChannelHandlerImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBinding(AuditConsumerChannels.class)
public class AuditJPAStreamsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditConsumerChannelHandler auditConsumerChannelHandler(EventsRepository eventsRepository,
                                                                   APIEventToEntityConverters eventConverters) {
        return new AuditConsumerChannelHandlerImpl(eventsRepository,
                                                   eventConverters);
    }

}
