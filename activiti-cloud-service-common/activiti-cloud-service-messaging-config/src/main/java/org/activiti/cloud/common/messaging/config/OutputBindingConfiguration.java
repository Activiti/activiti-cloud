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

import java.util.Optional;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.stream.binder.JavaClassMimeTypeUtils;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binding.DefaultPartitioningInterceptor;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.converter.MessageConverterUtils;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@AutoConfiguration(before = {BinderFactoryAutoConfiguration.class, FunctionBindingConfiguration.class, ConnectorConfiguration.class })
public class OutputBindingConfiguration extends AbstractFunctionalBindingConfiguration {

    public static final String OUTPUT_BINDING = "_source";

    @Bean
    public BeanPostProcessor outputBindingBeanPostProcessor(FunctionAnnotationService functionAnnotationService,
                                                            BindingServiceProperties bindingServiceProperties,
                                                            StreamFunctionProperties streamFunctionProperties,
                                                            DefaultListableBeanFactory beanFactory) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (MessageChannel.class.isInstance(bean)) {
                    Optional.ofNullable(functionAnnotationService.findAnnotationOnBean(beanName, OutputBinding.class))
                            .ifPresent(functionBinding -> {
                                String outputBinding = beanName + OUTPUT_BINDING;
                                final String beanOutName = getOutBinding(outputBinding);

                                String outputBindings = bindingServiceProperties.getOutputBindings();

                                if (!StringUtils.hasText(outputBindings)) {
                                    outputBindings = outputBinding;
                                } else {
                                    outputBindings += ";" + outputBinding;
                                }

                                bindingServiceProperties.setOutputBindings(outputBindings);

                                streamFunctionProperties.getBindings()
                                                        .put(beanOutName, beanName);

                                if (!DirectWithAttributesChannel.class.isInstance(bean)) {
                                    getMessageConverterConfigurer().configureOutputChannel(MessageChannel.class.cast(bean),
                                                                                           beanName);
                                }

                                CompositeMessageConverter messageConverter = getMessageConverter();

                                BindingProperties bindingProperties = bindingServiceProperties.getBindingProperties(beanName);

                                Optional.ofNullable(bindingProperties.getProducer())
                                    .filter(ProducerProperties::isPartitioned)
                                    .ifPresent(isPartitioned -> {
                                        InterceptableChannel.class.cast(bean)
                                                                  .addInterceptor(new DefaultPartitioningInterceptor(bindingProperties,
                                                                                                                     beanFactory));
                                    });

                                InterceptableChannel.class.cast(bean)
                                                          .addInterceptor(new OutboundContentTypeConvertingInterceptor("application/json",
                                                                                                                       messageConverter));

                            });
                }

                return bean;
            }
        };
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
            if (message instanceof ErrorMessage || isByteArrayWithContentType(message)) {
                return message;
            }

            String contentTypeFromHeader = message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE) ?
                message.getHeaders().get(MessageHeaders.CONTENT_TYPE).toString()
                : null;
            String contentTypeFromPayload = message.getPayload() instanceof String ?
                JavaClassMimeTypeUtils.mimeTypeFromObject(message.getPayload(),
                                                            ObjectUtils.nullSafeToString(contentTypeFromHeader)).toString()
                : contentTypeFromHeader;

            MessageHeaders messageHeaders = getMessageHeaders(message);

            @SuppressWarnings("unchecked")
            Message<byte[]> outboundMessage = getOutboundMessage(message, messageHeaders);

            MessageHeaders outboundMessageHeaders = getOutboundMessageHeaders(outboundMessage, contentTypeFromPayload, contentTypeFromHeader);

            return MessageBuilder.fromMessage(outboundMessage)
                                 .copyHeaders(outboundMessageHeaders)
                                 .build();
        }

        private boolean isByteArrayWithContentType(Message<?> message) {
            return message.getPayload() instanceof byte[]
                    && message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE);
        }

        private MessageHeaders getMessageHeaders(Message<?> message) {
            MessageHeaders messageHeaders = message.getHeaders();

            if (!message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)) {
                @SuppressWarnings("unchecked")
                MessageHeaderAccessor accessor = MessageHeaderAccessor.getMutableAccessor(message);
                accessor.setContentType(this.mimeType);
                messageHeaders = accessor.toMessageHeaders();
            }
            return messageHeaders;
        }

        private MessageHeaders getOutboundMessageHeaders(Message<?> outboundMessage, String contentTypeFromPayload, String contentTypeFromHeader) {
            MessageHeaders outboundMessageHeaders = outboundMessage.getHeaders();

            if (contentTypeFromPayload != null && !contentTypeFromPayload.equals(contentTypeFromHeader) && contentTypeFromHeader != null) {
                @SuppressWarnings("unchecked")
                MessageHeaderAccessor accessor = MessageHeaderAccessor.getMutableAccessor(outboundMessage);
                accessor.setContentType(MimeType.valueOf(contentTypeFromPayload));
                outboundMessageHeaders = accessor.toMessageHeaders();
            }
            return outboundMessageHeaders;
        }

        private Message<byte[]> getOutboundMessage(Message<?> message, MessageHeaders messageHeaders) {
            Message<byte[]> outboundMessage = message.getPayload() instanceof byte[]
                    ? (Message<byte[]>) message
                    : (Message<byte[]>) this.messageConverter.toMessage(message.getPayload(), messageHeaders);

            if (outboundMessage == null) {
                throw new IllegalStateException("Failed to convert message: '" + message
                        + "' to outbound message.");
            }
            return outboundMessage;
        }
    }

}
