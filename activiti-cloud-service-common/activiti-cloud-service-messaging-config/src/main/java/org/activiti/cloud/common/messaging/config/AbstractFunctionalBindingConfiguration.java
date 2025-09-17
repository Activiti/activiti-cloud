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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.activiti.cloud.common.messaging.functional.ConnectorGateway;
import org.activiti.cloud.common.messaging.functional.ConsumerGateway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.FunctionTypeUtils;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry;
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

    public FunctionInvocationWrapper functionWithCorrectedInput(FunctionInvocationWrapper function, Type inputType) {
        try {
            SimpleFunctionRegistry simpleFunctionRegistry = (SimpleFunctionRegistry) applicationContext.getBean(
                FunctionRegistry.class
            );
            Constructor<?>[] constructors =
                SimpleFunctionRegistry.FunctionInvocationWrapper.class.getDeclaredConstructors();

            Constructor<?> targetConstructor = null;
            for (Constructor<?> constructor : constructors) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length >= 4) {
                    targetConstructor = constructor;
                    break;
                }
            }

            if (targetConstructor == null) {
                throw new IllegalStateException("Cannot find suitable constructor for FunctionInvocationWrapper");
            }

            targetConstructor.setAccessible(true);

            return (FunctionInvocationWrapper) targetConstructor.newInstance(
                simpleFunctionRegistry,
                function.getFunctionDefinition(),
                function.getTarget(),
                inputType, // This MUST be the new input type (Payload.class)
                function.getOutputType()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create function with corrected input type: " + inputType, e);
        }
    }

    /**
     * Alternative approach: Create a functional wrapper with the correct input type
     * This version ensures we always change the input type and never return the original
     */
    private FunctionInvocationWrapper createFunctionWrapperAlternative(
        FunctionInvocationWrapper originalFunction,
        Type inputType
    ) {
        try {
            // Get the function registry
            FunctionRegistry functionRegistry = applicationContext.getBean(FunctionRegistry.class);

            if (functionRegistry instanceof SimpleFunctionRegistry) {
                SimpleFunctionRegistry simpleFunctionRegistry = (SimpleFunctionRegistry) functionRegistry;

                // Try alternative reflection approach to create wrapper without registration
                Constructor<?>[] constructors =
                    SimpleFunctionRegistry.FunctionInvocationWrapper.class.getDeclaredConstructors();

                for (Constructor<?> constructor : constructors) {
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    // Try different constructor signatures (5 parameters, 6 parameters, etc.)
                    if (paramTypes.length >= 4 && paramTypes.length <= 6) {
                        try {
                            constructor.setAccessible(true);

                            // Try to instantiate with the available parameters
                            if (paramTypes.length == 4) {
                                return (FunctionInvocationWrapper) constructor.newInstance(
                                    originalFunction.getFunctionDefinition(),
                                    originalFunction.getTarget(),
                                    inputType, // Use the new input type (Payload.class)
                                    originalFunction.getOutputType()
                                );
                            } else if (paramTypes.length == 5) {
                                return (FunctionInvocationWrapper) constructor.newInstance(
                                    simpleFunctionRegistry,
                                    originalFunction.getFunctionDefinition(),
                                    originalFunction.getTarget(),
                                    inputType, // Use the new input type (Payload.class)
                                    originalFunction.getOutputType()
                                );
                            } else if (paramTypes.length == 6) {
                                return (FunctionInvocationWrapper) constructor.newInstance(
                                    simpleFunctionRegistry,
                                    originalFunction.getFunctionDefinition(),
                                    originalFunction.getTarget(),
                                    inputType, // Use the new input type (Payload.class)
                                    originalFunction.getOutputType(),
                                    null // Additional parameter (often for function properties)
                                );
                            }
                        } catch (Exception constructorException) {
                            // Try next constructor
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to create alternative function wrapper with input type: " + inputType,
                e
            );
        }

        // If all reflection attempts fail, throw an exception instead of returning original
        throw new IllegalStateException(
            "Cannot create function with correct input type: " + inputType + ". All reflection approaches failed."
        );
    }

    /**
     * Creates a function with the correct input type and returns the FunctionInvocationWrapper.
     * This method uses existing registered functions and relies on reflection for type correction.
     *
     * @param originalFunction The original function object to wrap
     * @param newFunctionName The name for the new function registration
     * @param inputType The desired input type (e.g., Payload.class)
     * @return FunctionInvocationWrapper with correct type binding
     */
    protected FunctionInvocationWrapper createFunctionWithCorrectType(
        Object originalFunction,
        String newFunctionName,
        Type inputType
    ) {
        // Get the function registry
        FunctionRegistry functionRegistry = applicationContext.getBean(FunctionRegistry.class);

        // First, try to find the existing function using multiple strategies
        FunctionInvocationWrapper existingFunction = null;
        try {
            existingFunction = functionFromDefinition(newFunctionName);
        } catch (Exception e) {
            // Try alternative lookup strategies
            try {
                existingFunction = functionRegistry.lookup(newFunctionName);
            } catch (Exception lookupException) {
                // Try with registration suffix
                try {
                    existingFunction = functionRegistry.lookup(newFunctionName + REGISTRATION_NAME_SUFFIX);
                } catch (Exception suffixException) {
                    throw new IllegalStateException(
                        "Cannot find function with name: " +
                        newFunctionName +
                        ". Tried: '" +
                        newFunctionName +
                        "', '" +
                        newFunctionName +
                        REGISTRATION_NAME_SUFFIX +
                        "'",
                        e
                    );
                }
            }
        }

        // Now that we have the existing function, create a new one with the correct input type
        if (existingFunction != null) {
            return functionWithCorrectedInput(existingFunction, inputType);
        }

        throw new IllegalStateException("Cannot find function with name: " + newFunctionName);
    }

    /**
     * Creates a function wrapper with Payload input/output type.
     * This is a convenience method for the common case of Payload-to-Payload functions.
     *
     * @param originalFunction The original function object
     * @param functionName The name for the function
     * @return FunctionInvocationWrapper with Payload input/output types
     */
    protected FunctionInvocationWrapper createPayloadFunction(Object originalFunction, String functionName) {
        return createFunctionWithCorrectType(
            originalFunction,
            functionName,
            org.activiti.api.model.shared.Payload.class
        );
    }

    /**
     * Creates a function with the correct input type and returns the FunctionInvocationWrapper.
     * This method uses existing registered functions and relies on reflection for type correction.
     *
     * @param originalFunction The original FunctionInvocationWrapper
     * @param functionName The name for the function
     * @param inputType The desired input type (e.g., Payload.class)
     * @return FunctionInvocationWrapper with correct input type binding
     */
    protected FunctionInvocationWrapper createFunctionWithCorrectInputType(
        FunctionInvocationWrapper originalFunction,
        String functionName,
        Type inputType
    ) {
        // Use the existing functionWithCorrectedInput method which uses reflection
        // This avoids temporary registrations while still changing the input type
        return functionWithCorrectedInput(originalFunction, inputType);
    }

    protected Type discoverFunctionType(Object bean, String beanName) {
        return FunctionTypeUtils.discoverFunctionType(
            bean,
            beanName,
            GenericApplicationContext.class.cast(applicationContext)
        );
    }

    protected String registerFunctionRegistration(String functionName, FunctionRegistration functionRegistration) {
        final String beanName = functionName + REGISTRATION_NAME_SUFFIX;

        functionRegistration.setBeanName(beanName);

        GenericApplicationContext.class.cast(applicationContext)
            .registerBean(beanName, FunctionRegistration.class, () -> functionRegistration);

        return beanName;
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
