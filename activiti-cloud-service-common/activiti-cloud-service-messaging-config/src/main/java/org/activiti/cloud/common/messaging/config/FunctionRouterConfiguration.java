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

import org.activiti.cloud.common.messaging.functional.InputBinding;
import org.activiti.cloud.common.messaging.functional.OutputBinding;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.SubscribableChannel;

@AutoConfiguration(before = InputBindingConfiguration.class, after = BinderFactoryAutoConfiguration.class)
@ConditionalOnProperty("activiti.cloud.messaging.function-router.enabled")
public class FunctionRouterConfiguration {

    public static final String FUNCTION_ROUTER_INPUT = "functionRouterInput";
    public static final String FUNCTION_ROUTER_OUTPUT = "functionRouterOutput";

    @InputBinding(FUNCTION_ROUTER_INPUT)
    SubscribableChannel functionRouterInput() {
        return MessageChannels.publishSubscribe(FUNCTION_ROUTER_INPUT).getObject();
    }

    @OutputBinding(FUNCTION_ROUTER_OUTPUT)
    SubscribableChannel functionRouterOutput() {
        return MessageChannels.direct(FUNCTION_ROUTER_OUTPUT).getObject();
    }
}
