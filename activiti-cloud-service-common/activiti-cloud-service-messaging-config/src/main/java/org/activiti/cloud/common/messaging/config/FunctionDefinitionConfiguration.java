package org.activiti.cloud.common.messaging.config;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.config.ConnectorConfiguration.ConnectorMessageFunction;
import org.activiti.cloud.common.messaging.functional.ConditionFunctionDefinition;
import org.activiti.cloud.common.messaging.functional.FunctionDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@Configuration
public class FunctionDefinitionConfiguration {

    private ExpressionParser expressionParser = new SpelExpressionParser();

    private GenericHandler<Message<?>> handler(Object bean){
        return message -> {
            if(Consumer.class.isInstance(bean)){
                Consumer<Message<?>> consumer = (Consumer<Message<?>>) bean;
                consumer.accept(message);
            } else if (Function.class.isInstance(bean)){
                Function
            }
        };
    }

    @Bean
    public BeanPostProcessor functionalBeanPostProcessor(DefaultListableBeanFactory beanFactory,
        IntegrationFlowContext integrationFlowContext,
        StreamFunctionProperties streamFunctionProperties,
        StreamBridge streamBridge,
        FunctionDefinitionPropertySource functionDefinitionPropertySource) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, FunctionDefinition.class))
                    .ifPresent(functionDefinition -> {
                        Optional.of(functionDefinition.output())
                            .filter(StringUtils::hasText)
                            .ifPresent(output -> {
                                streamFunctionProperties.getBindings()
                                    .put(beanName + "-out-0", output);
                            });

                        Optional.of(functionDefinition.input())
                            .filter(StringUtils::hasText)
                            .ifPresent(input -> {
                                streamFunctionProperties.getBindings()
                                    .put(beanName + "-in-0", input);
                            });

                        Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, ConditionFunctionDefinition.class))
                            .ifPresent(conditionDefinition -> {
                                if(StringUtils.hasText(conditionDefinition.condition())) {
                                    IntegrationFlow flow = IntegrationFlows.from(ConditionalConsumer.class,
                                            (gateway) -> gateway.beanName(beanName)
                                                .replyTimeout(0L))
                                        .log(LoggingHandler.Level.INFO, beanName)
                                        .filter(conditionDefinition.condition())
                                        .handle(Message.class, )
                                        .get();

                                    integrationFlowContext.registration(flow)
                                        .register();
                                }
                            });
                    });

                return bean;
            }
        };
    }

    public interface ConditionalConsumer extends Consumer<Message<?>> {

        @Override
        void accept(Message<?> message) throws MessagingException;
    }

}
