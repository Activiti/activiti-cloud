/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.conf.activiti.services.connectors;

import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;
import org.conf.activiti.runtime.api.ConnectorsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(value = ConnectorsAutoConfiguration.class)
@PropertySource("classpath:config/integration-result-stream.properties")
public class CloudConnectorsAutoConfiguration {

    @Bean(name = DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME)
    @ConditionalOnMissingBean(name = DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME)
    public MQServiceTaskBehavior mqServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                                       ApplicationEventPublisher eventPublisher,
                                                       ApplicationContext applicationContext,
                                                       IntegrationContextBuilder integrationContextBuilder,
                                                       RuntimeBundleInfoAppender runtimeBundleInfoAppender) {
        return new MQServiceTaskBehavior(integrationContextManager,
                                         eventPublisher,
                                         applicationContext,
                                         integrationContextBuilder,
                                         runtimeBundleInfoAppender);
    }
}
