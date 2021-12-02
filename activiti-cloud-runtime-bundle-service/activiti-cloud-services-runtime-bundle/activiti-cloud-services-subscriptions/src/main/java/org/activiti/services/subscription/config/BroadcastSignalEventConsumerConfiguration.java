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
package org.activiti.services.subscription.config;

import java.util.function.Consumer;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.engine.RuntimeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "activiti.stream.cloud.functional.binding", havingValue = "enabled")
public class BroadcastSignalEventConsumerConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "signalConsumer")
    public Consumer<SignalPayload> signalConsumer(RuntimeService runtimeService) {
        return (signalPayload) -> {
            if ((signalPayload.getVariables() == null) || (signalPayload.getVariables().isEmpty())) {
                runtimeService.signalEventReceived(signalPayload.getName());
            } else {
                runtimeService.signalEventReceived(signalPayload.getName(),
                        signalPayload.getVariables());
            }
        };
    }
}
