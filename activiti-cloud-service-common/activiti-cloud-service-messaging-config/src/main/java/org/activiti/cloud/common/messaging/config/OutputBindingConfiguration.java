/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.common.messaging.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.function.context.config.JsonMessageConverter;
import org.springframework.cloud.function.context.config.SmartCompositeMessageConverter;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.cloud.function.utils.PrimitiveTypesFromStringMessageConverter;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.JavaClassMimeTypeUtils;
import org.springframework.cloud.stream.binder.PartitionHandler;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binding.MessageConverterConfigurer;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.converter.MessageConverterUtils;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Configuration
public class OutputBindingConfiguration {

    @Bean
    public BeanPostProcessor outputBindingBeanPostProcessor(DefaultListableBeanFactory beanFactory,
                                                            BindingServiceProperties bindingServiceProperties,
                                                            StreamFunctionProperties streamFunctionProperties,
                                                            MessageConverterConfigurer messageConverterConfigurer) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (MessageChannel.class.isInstance(bean)) {

                    Optional.ofNullable(beanFactory.findAnnotationOnBean(beanName, OutputBinding.class))
                            .ifPresent(functionBinding -> {

                                Optional.of(beanName)
                                        .ifPresent(output -> {
                                            String outputBinding = output + "Supplier";
                                            String outputBindings = bindingServiceProperties.getOutputBindings();

                                            if (!StringUtils.hasText(outputBindings)) {
                                                outputBindings = outputBinding;
                                            } else {
                                                outputBindings += ";" + outputBinding;
                                            }

                                            streamFunctionProperties.getBindings()
                                                                    .put(outputBinding + "-out-0", beanName);

                                            bindingServiceProperties.setOutputBindings(outputBindings);

                                            if (!DirectWithAttributesChannel.class.isInstance(bean)) {
                                                messageConverterConfigurer.configureOutputChannel(MessageChannel.class.cast(bean),
                                                                                                  beanName);
                                            }

                                            CompositeMessageConverter messageConverter = getMessageConverter(beanFactory);

                                            BindingProperties bindingProperties = bindingServiceProperties.getBindingProperties(beanName);

                                            Optional.ofNullable(bindingProperties.getProducer())
                                                .filter(ProducerProperties::isPartitioned)
                                                .ifPresent(isPartitioned -> {
                                                    InterceptableChannel.class.cast(bean)
                                                                              .addInterceptor(new PartitioningInterceptor(bindingProperties,
                                                                                                                          beanFactory));
                                                });

                                            InterceptableChannel.class.cast(bean)
                                                                      .addInterceptor(new OutboundContentTypeConvertingInterceptor("application/json",
                                                                                                                                   messageConverter));

                                        });
                            });
                }

                return bean;
            }
        };
    }

    private static CompositeMessageConverter getMessageConverter(BeanFactory beanFactory) {
        List<MessageConverter> messageConverters = new ArrayList<>();
        JsonMapper jsonMapper = beanFactory.getBean(JsonMapper.class);

        messageConverters.add(new JsonMessageConverter(jsonMapper));
        messageConverters.add(new ByteArrayMessageConverter());
        messageConverters.add(new StringMessageConverter());
        messageConverters.add(new PrimitiveTypesFromStringMessageConverter(new DefaultConversionService()));

        return new SmartCompositeMessageConverter(messageConverters);
    }

    /**
     * Unlike INBOUND where the target type is known and conversion is typically done by
     * argument resolvers of {@link InvocableHandlerMethod} for the OUTBOUND case it is
     * not known so we simply rely on provided MessageConverters that will use the
     * provided 'contentType' and convert messages to a type dictated by the Binders
     * (i.e., byte[]).
     */
    private final class OutboundContentTypeConvertingInterceptor implements ChannelInterceptor {

        final MimeType mimeType;

        private final MessageConverter messageConverter;

        private OutboundContentTypeConvertingInterceptor(String contentType,
                                                         CompositeMessageConverter messageConverter) {
            this.mimeType = MessageConverterUtils.getMimeType(contentType);
            this.messageConverter = messageConverter;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            if (message instanceof ErrorMessage) {
                return message;
            }

            if (message.getPayload() instanceof byte[]
                && message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)) {
                return message;
            }

            String oct = message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)
                ? message.getHeaders().get(MessageHeaders.CONTENT_TYPE).toString()
                : null;
            String ct = message.getPayload() instanceof String
                ? JavaClassMimeTypeUtils.mimeTypeFromObject(message.getPayload(),
                                                            ObjectUtils.nullSafeToString(oct)).toString()
                : oct;

            MessageHeaders messageHeaders = message.getHeaders();

            if (!message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)) {
                @SuppressWarnings("unchecked")
                MessageHeaderAccessor accessor = MessageHeaderAccessor.getMutableAccessor(message);
                accessor.setContentType(this.mimeType);
                messageHeaders = accessor.toMessageHeaders();
            }

            @SuppressWarnings("unchecked")
            Message<byte[]> outboundMessage = message.getPayload() instanceof byte[]
                ? (Message<byte[]>) message : (Message<byte[]>) this.messageConverter.toMessage(message.getPayload(), messageHeaders);

            if (outboundMessage == null) {
                throw new IllegalStateException("Failed to convert message: '" + message
                                                    + "' to outbound message.");
            }

            MessageHeaders outboundMessageHeaders = outboundMessage.getHeaders();

            if (ct != null && !ct.equals(oct) && oct != null) {
                @SuppressWarnings("unchecked")
                MessageHeaderAccessor accessor = MessageHeaderAccessor.getMutableAccessor(outboundMessage);
                accessor.setContentType(MimeType.valueOf(ct));
                outboundMessageHeaders = accessor.toMessageHeaders();
            }
            return MessageBuilder.fromMessage(outboundMessage)
                                 .copyHeaders(outboundMessageHeaders)
                                 .build();
        }

    }

    final class PartitioningInterceptor implements ChannelInterceptor {

        private final BindingProperties bindingProperties;

        private final PartitionHandler partitionHandler;

        public PartitioningInterceptor(BindingProperties bindingProperties,
                                       ConfigurableListableBeanFactory beanFactory) {
            this.bindingProperties = bindingProperties;
            this.partitionHandler = new PartitionHandler(
                ExpressionUtils.createStandardEvaluationContext(beanFactory),
                this.bindingProperties.getProducer(), beanFactory);
        }

        public void setPartitionCount(int partitionCount) {
            this.partitionHandler.setPartitionCount(partitionCount);
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            if (!message.getHeaders().containsKey(BinderHeaders.PARTITION_OVERRIDE)) {
                int partition = this.partitionHandler.determinePartition(message);
                return MessageBuilder.fromMessage(message)
                                     .setHeader(BinderHeaders.PARTITION_HEADER, partition)
                                     .build();
            }
            else {
                return MessageBuilder.fromMessage(message)
                                        .setHeader(BinderHeaders.PARTITION_HEADER, message.getHeaders()
                                                                                          .get(BinderHeaders.PARTITION_OVERRIDE))
                    .removeHeader(BinderHeaders.PARTITION_OVERRIDE).build();
            }
        }

    }
}
