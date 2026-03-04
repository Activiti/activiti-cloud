/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.el.ExpressionFactory;
import java.util.List;
import java.util.Map;
import org.activiti.cloud.common.messaging.ActivitiCloudMessagingProperties;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.liquibase.EnableCleanupLiquibaseAfterTest;
import org.activiti.cloud.starters.test.binder.BinderFactoryListenerTestContext;
import org.activiti.cloud.starters.test.binder.EnableBinderFactoryListenerTestContext;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.TaskEntityImpl;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootTest(
    classes = RuntimeBundleApplication.class,
    properties = {
        "activiti.cloud.application.name=default-app",
        "activiti.cloud.messaging.rabbitmq.compress=true",
        "activiti.cloud.messaging.rabbitmq.compression-level=9",
    }
)
@EnableCleanupLiquibaseAfterTest
@EnableBinderFactoryListenerTestContext
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@ResourceLocks(value = { @ResourceLock("rabbitmq"), @ResourceLock("postgres") })
public class RuntimeBundleApplicationIT {

    @ServiceConnection
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.8.6-management-alpine").withReuse(true);

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withReuse(true);

    @Autowired
    protected BinderFactoryListenerTestContext binderFactoryListenerTestContext;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private ActivitiCloudMessagingProperties messagingProperties;

    @Autowired
    private ExpressionManager expressionManager;

    @Autowired
    private ProcessEngineConfigurationImpl processEngineConfiguration;

    @Test
    public void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void expressionFactory() {
        final var expressionFactory = ExpressionFactory.newInstance();

        assertThat(expressionFactory).isInstanceOf(org.apache.el.ExpressionFactoryImpl.class);
    }

    @Test
    void expressionManager() {
        try {
            Context.setProcessEngineConfiguration(processEngineConfiguration);
            Context.setCommandContext(new CommandContext(commandContext -> null, processEngineConfiguration));

            final var value = expressionManager.createExpression("${[]}").getValue(new TaskEntityImpl());

            assertThat(value).isNotNull().isInstanceOf(List.class);

            final var value2 = expressionManager
                .createExpression("${[var]}")
                .getValue(expressionManager, new DefaultDelegateInterceptor(), Map.of("var", "foo"));

            assertThat(value2).isNotNull().asInstanceOf(InstanceOfAssertFactories.list(Object.class)).contains("foo");

            final var value3 = expressionManager
                .createExpression("${(var ne [])}")
                .getValue(expressionManager, new DefaultDelegateInterceptor(), Map.of("var", List.of("foo")));

            assertThat(value3).isNotNull().isInstanceOf(Boolean.class).isEqualTo(true);
        } finally {
            Context.removeCommandContext();
            Context.removeProcessEngineConfiguration();
        }
    }

    @Test
    void rabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getQueues())
            .isNotEmpty()
            .containsOnlyKeys(
                "engineEvents.query",
                "engineEvents.audit",
                "messageEvents_default-app.messages",
                "signalEvent.my-runtime-bundle",
                "commandConsumer_default-app.my-runtime-bundle",
                "asyncExecutorJobs_default-app.my-runtime-bundle",
                "integrationResult_my-runtime-bundle.my-runtime-bundle",
                "integrationError_my-runtime-bundle.my-runtime-bundle"
            );
    }

    @Test
    void anonymousRabbitQueues() {
        assertThat(binderFactoryListenerTestContext.getAnonymousQueues()).isEmpty();
    }

    @Test
    void rabbitExchanges() {
        assertThat(binderFactoryListenerTestContext.getExchanges())
            .isNotEmpty()
            .containsOnlyKeys(
                "commandResults_default-app",
                "engineEvents",
                "asyncExecutorJobs_default-app",
                "commandConsumer_default-app",
                "messageEvents_default-app",
                "signalEvent",
                "integrationResult_my-runtime-bundle",
                "integrationError_my-runtime-bundle"
            );
    }

    @Test
    void rabbitBinderCompression() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.binder.compression-level", Integer.class))
            .isEqualTo(9);
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.compress", Boolean.class))
            .isTrue();
    }

    @Test
    void messagingPropertiesRabbitMqCompression() {
        assertThat(messagingProperties.getRabbitmq().getCompressionLevel()).isEqualTo(9);
        assertThat(messagingProperties.getRabbitmq().isCompress()).isTrue();
    }

    @Test
    void messagingRabbitMqPrefixProperties() {
        assertThat(messagingProperties.getRabbitmq().getPrefix()).isNullOrEmpty();
    }

    @Test
    void rabbitBinderDefaultPrefix() {
        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.consumer.prefix", String.class))
            .isNullOrEmpty();

        assertThat(environment.getProperty("spring.cloud.stream.rabbit.default.producer.prefix", String.class))
            .isNullOrEmpty();
    }
}
