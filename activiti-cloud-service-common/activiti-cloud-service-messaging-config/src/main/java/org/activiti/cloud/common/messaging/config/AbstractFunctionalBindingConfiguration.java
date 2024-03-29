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

import static org.springframework.cloud.function.context.FunctionRegistration.REGISTRATION_NAME_SUFFIX;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.common.messaging.functional.ConnectorGateway;
import org.activiti.cloud.common.messaging.functional.ConsumerGateway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.FunctionTypeUtils;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.function.context.config.JsonMessageConverter;
import org.springframework.cloud.function.context.config.SmartCompositeMessageConverter;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.cloud.function.utils.PrimitiveTypesFromStringMessageConverter;
import org.springframework.cloud.stream.binding.MessageConverterConfigurer;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.Assert;

public abstract class AbstractFunctionalBindingConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private SmartCompositeMessageConverter smartCompositeMessageConverter;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.notNull(
            applicationContext,
            this.getClass().getSimpleName() + " can not process beans because the application context is null"
        );
        this.applicationContext = applicationContext;
    }

    protected StreamBridge getStreamBridge() {
        return this.applicationContext.getBean(StreamBridge.class);
    }

    public static String getOutBinding(String bindingName) {
        return getOutBinding(bindingName, 0);
    }

    public static String getOutBinding(String bindingName, int arity) {
        return String.format("%s-out-%d", bindingName, arity);
    }

    public static String getInBinding(String bindingName) {
        return getInBinding(bindingName, 0);
    }

    public static String getInBinding(String bindingName, int arity) {
        return String.format("%s-in-%d", bindingName, arity);
    }

    protected Class<?> getGatewayInterface(boolean hasOutput) {
        if (hasOutput) {
            return ConnectorGateway.class;
        } else {
            return ConsumerGateway.class;
        }
    }

    protected FunctionInvocationWrapper functionFromDefinition(String definition) {
        FunctionRegistry functionRegistry = applicationContext.getBean(FunctionRegistry.class);
        FunctionInvocationWrapper function = functionRegistry.lookup(definition + REGISTRATION_NAME_SUFFIX);
        Assert.notNull(function, "Failed to lookup function '" + definition + "'");
        return function;
    }

    protected Type discoverFunctionType(Object bean, String beanName) {
        return FunctionTypeUtils.discoverFunctionType(
            bean,
            beanName,
            GenericApplicationContext.class.cast(applicationContext)
        );
    }

    protected void registerFunctionRegistration(String functionName, FunctionRegistration functionRegistration) {
        GenericApplicationContext.class.cast(applicationContext)
            .registerBean(
                functionName + REGISTRATION_NAME_SUFFIX,
                FunctionRegistration.class,
                () -> functionRegistration
            );
    }

    protected CompositeMessageConverter getMessageConverter() {
        synchronized (this) {
            if (smartCompositeMessageConverter == null) {
                BeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();

                List<MessageConverter> messageConverters = new ArrayList<>();
                JsonMapper jsonMapper = beanFactory.getBean(JsonMapper.class);

                messageConverters.add(new JsonMessageConverter(jsonMapper));
                messageConverters.add(new ByteArrayMessageConverter());
                messageConverters.add(new StringMessageConverter());
                messageConverters.add(new PrimitiveTypesFromStringMessageConverter(new DefaultConversionService()));

                this.smartCompositeMessageConverter = new SmartCompositeMessageConverter(messageConverters);
            }
        }

        return this.smartCompositeMessageConverter;
    }

    protected MessageConverterConfigurer getMessageConverterConfigurer() {
        return applicationContext.getBean("messageConverterConfigurer", MessageConverterConfigurer.class);
    }
}
