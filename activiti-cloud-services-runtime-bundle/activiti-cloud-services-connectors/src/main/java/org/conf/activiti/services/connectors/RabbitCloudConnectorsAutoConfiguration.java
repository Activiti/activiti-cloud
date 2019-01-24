package org.conf.activiti.services.connectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitProducerProperties;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver.NewDestinationBindingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Configures routing key expression for dynamic cloud connector destinations if rabbit binder exists on classpath
 */
@Configuration
@ConditionalOnClass(RabbitProducerProperties.class)
@AutoConfigureAfter(CloudConnectorsAutoConfiguration.class)
public class RabbitCloudConnectorsAutoConfiguration {

    @Value("${activiti.spring.cloud.stream.connector.integrationRequestSender.routing-key-expression:headers['routingKey']}")
    private String routingKeyExpression;
    
    @Bean
    @ConditionalOnMissingBean
    public NewDestinationBindingCallback<RabbitProducerProperties> dynamicConnectorDestinationsBindingCallback() {
        return (channelName, channel, producerProperties, extendedProducerProperties) -> {
            Expression expression = new SpelExpressionParser().parseExpression(routingKeyExpression);
            
            extendedProducerProperties.setRoutingKeyExpression(expression);
        };
    }
}
