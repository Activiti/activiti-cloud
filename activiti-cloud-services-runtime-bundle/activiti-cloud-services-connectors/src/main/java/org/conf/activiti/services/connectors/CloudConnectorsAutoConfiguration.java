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

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.activiti.engine.impl.persistence.entity.integration.IntegrationContextManager;
import org.activiti.runtime.api.connector.ConnectorActionDefinitionFinder;
import org.activiti.runtime.api.connector.DefaultServiceTaskBehavior;
import org.activiti.runtime.api.connector.IntegrationContextBuilder;
import org.activiti.runtime.api.connector.OutboundVariablesProvider;
import org.activiti.services.connectors.behavior.MQServiceTaskBehavior;
import org.activiti.services.connectors.message.IntegrationContextMessageBuilderFactory;
import org.conf.activiti.runtime.api.ConnectorsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(value = ConnectorsAutoConfiguration.class)
@ComponentScan("org.activiti.core.common.spring.connector")
@PropertySource("classpath:config/integration-result-stream.properties")
public class CloudConnectorsAutoConfiguration {

    private static final String LOCAL_SERVICE_TASK_BEHAVIOUR_BEAN_NAME = "localServiceTaskBehaviour";

    @Bean
    @ConditionalOnMissingBean
    public IntegrationContextMessageBuilderFactory integrationContextMessageBuilderFactory(RuntimeBundleProperties properties) {
        return new IntegrationContextMessageBuilderFactory(properties);
    }

    @Bean(name = LOCAL_SERVICE_TASK_BEHAVIOUR_BEAN_NAME)
    @ConditionalOnMissingBean(name = LOCAL_SERVICE_TASK_BEHAVIOUR_BEAN_NAME)
    public DefaultServiceTaskBehavior localServiceTaskBehavior(ApplicationContext applicationContext,
                                                               ConnectorActionDefinitionFinder connectorActionDefinitionFinder,
                                                               IntegrationContextBuilder integrationContextBuilder,
                                                               OutboundVariablesProvider outboundVariablesProvider) {
        // this bean is exposed under two different names (LOCAL_SERVICE_TASK_BEHAVIOUR_BEAN_NAME and
        // DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME) to allow MQServiceTaskBehavior
        // to use composition instead of inheritance, this will make maintenance easier as changes in constructor
        // of DefaultServiceTaskBehavior will not impact the constructor of MQServiceTaskBehavior.
        // LOCAL_SERVICE_TASK_BEHAVIOUR_BEAN_NAME will be injected in MQServiceTaskBehavior;
        // DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME will be available only in non-cloud environment:
        // MQServiceTaskBehavior will replace it for cloud environment.
        return new DefaultServiceTaskBehavior(applicationContext,
                                              integrationContextBuilder,
                                              connectorActionDefinitionFinder,
                                              outboundVariablesProvider);
    }
    
    @Bean(name = DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME)
    @ConditionalOnMissingBean(name = DefaultActivityBehaviorFactory.DEFAULT_SERVICE_TASK_BEAN_NAME)
    public MQServiceTaskBehavior mqServiceTaskBehavior(IntegrationContextManager integrationContextManager,
                                                       ApplicationEventPublisher eventPublisher,
                                                       IntegrationContextBuilder integrationContextBuilder,
                                                       RuntimeBundleInfoAppender runtimeBundleInfoAppender,
                                                       DefaultServiceTaskBehavior defaultServiceTaskBehavior) {
        return new MQServiceTaskBehavior(integrationContextManager,
                                         eventPublisher,
                                         integrationContextBuilder,
                                         runtimeBundleInfoAppender,
                                         defaultServiceTaskBehavior);
    }
}
