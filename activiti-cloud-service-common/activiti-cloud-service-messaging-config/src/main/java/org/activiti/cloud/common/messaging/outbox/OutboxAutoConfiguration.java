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
package org.activiti.cloud.common.messaging.outbox;

import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.activiti.cloud.common.messaging.config.OutputBindingConfiguration;
import org.activiti.cloud.common.messaging.config.StreamBridgeOperations;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cloud.stream.binding.NewDestinationBindingCallback;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.ChannelMessageStorePreparedStatementSetter;
import org.springframework.integration.jdbc.store.channel.ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.MessageRowMapper;
import org.springframework.integration.jdbc.store.channel.OracleChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration(after = OutputBindingConfiguration.class)
@ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.enabled")
@ConditionalOnClass({ DataSource.class, JdbcChannelMessageStore.class })
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    EmbeddedHeadersJsonMessageMapper embeddedHeadersJsonMessageMapper() {
        return new EmbeddedHeadersJsonMessageMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    AllowListDeserializingConverter allowListDeserializingConverter(
        EmbeddedHeadersJsonMessageMapper embeddedHeadersJsonMessageMapper
    ) {
        return new AllowListDeserializingConverter(new OutboxMessageDeserializer(embeddedHeadersJsonMessageMapper));
    }

    @Bean
    @ConditionalOnMissingBean
    MessageRowMapper messageRowMapperProvider(AllowListDeserializingConverter allowListDeserializingConverter) {
        return new MessageRowMapper(allowListDeserializingConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    SerializingConverter serializingConverter(EmbeddedHeadersJsonMessageMapper embeddedHeadersJsonMessageMapper) {
        return new SerializingConverter(new OutboxMessageSerializer(embeddedHeadersJsonMessageMapper));
    }

    @Bean
    @ConditionalOnMissingBean
    ChannelMessageStorePreparedStatementSetter channelMessageStorePreparedStatementSetterProvider(
        SerializingConverter serializingConverter
    ) {
        return new ChannelMessageStorePreparedStatementSetter(serializingConverter);
    }

    @Bean
    OutboxChannelInterceptorFactory outboxChannelInterceptorFactory(
        ChannelMessageStore outboxChannelMessageStore,
        PlatformTransactionManager transactionManager,
        IntegrationFlowContext integrationFlowContext,
        ObjectProvider<StreamBridge> streamBridgeProvider,
        LockRegistry lockRegistry
    ) {
        return new OutboxChannelInterceptorFactory(
            outboxChannelMessageStore,
            transactionManager,
            integrationFlowContext,
            streamBridgeProvider,
            lockRegistry
        );
    }

    @Bean
    NewDestinationBindingCallback<Object> outboxDestinationBindingCallback(
        OutboxChannelInterceptorFactory outboxChannelInterceptorFactory
    ) {
        final var channelInterceptor = outboxChannelInterceptorFactory.apply("streamBridge", (m, c) ->
            c.getComponentName()
        );

        return new OutboxDestinationBindingCallback(channelInterceptor);
    }

    @Bean
    BeanPostProcessor outboxOutputBindingPostProcessor(
        DefaultListableBeanFactory beanFactory,
        OutboxChannelInterceptorFactory outboxChannelInterceptorFactory
    ) {
        return new OutboxOutputBindingPostProcessor(beanFactory, outboxChannelInterceptorFactory);
    }

    @Bean
    BeanPostProcessor outboxStreamBridgeOperationsPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof StreamBridgeOperations) {
                    ProxyFactory factory = new ProxyFactory(bean);
                    factory.setProxyTargetClass(true); // Forces CGLIB subclass proxy
                    factory.addAdvice(new OutboxStreamBridgeAdvice());

                    return factory.getProxy();
                }

                return bean;
            }
        };
    }

    @Bean
    JdbcChannelMessageStore outboxChannelMessageStore(
        DataSource dataSource,
        ChannelMessageStorePreparedStatementSetter channelMessageStorePreparedStatementSetter,
        MessageRowMapper messageRowMapper,
        ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider,
        @Value("${activiti.cloud.messaging.outbox.region:${spring.application.name:default}}") String region
    ) {
        final var messageStore = new JdbcChannelMessageStore(dataSource);

        messageStore.setUsingIdCache(true);
        messageStore.setChannelMessageStoreQueryProvider(channelMessageStoreQueryProvider);
        messageStore.setCheckDatabaseOnStart(true);
        messageStore.setPreparedStatementSetter(channelMessageStorePreparedStatementSetter);
        messageStore.setMessageRowMapper(messageRowMapper);
        messageStore.setRegion(region);

        return messageStore;
    }

    @Bean
    @ConditionalOnMissingBean
    ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider(DataSource dataSource)
        throws MetaDataAccessException {
        final var databaseName = JdbcUtils.extractDatabaseMetaData(
            dataSource,
            DatabaseMetaData::getDatabaseProductName
        );

        return switch (databaseName.toLowerCase()) {
            case "postgresql" -> new PostgresChannelMessageStoreQueryProvider();
            case "h2" -> new H2ChannelMessageStoreQueryProvider();
            case "oracle" -> new OracleChannelMessageStoreQueryProvider();
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseName);
        };
    }

    @Bean
    OutboxManager outboxManager(SmartLifecycleRoleController smartLifecycleRoleController) {
        return new OutboxManager(smartLifecycleRoleController);
    }

    @Configuration
    @ConditionalOnClass({ Endpoint.class })
    @ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.actuator.enabled", matchIfMissing = true)
    static class OutboxActuatorConfiguration {

        @Bean
        OutboxActuatorEndpoint outboxActuatorEndpoint(SmartLifecycleRoleController smartLifecycleRoleController) {
            return new OutboxActuatorEndpoint(smartLifecycleRoleController);
        }
    }

    @Configuration
    @ConditionalOnClass({ HealthIndicator.class })
    @ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.health.enabled", matchIfMissing = true)
    static class OutboxHealthIndicatorConfiguration {

        @Bean
        OutboxHealthIndicator outboxHealthIndicator(
            JdbcChannelMessageStore outboxQueueChannelMessageStore,
            SmartLifecycleRoleController smartLifecycleRoleController
        ) {
            return new OutboxHealthIndicator(outboxQueueChannelMessageStore, smartLifecycleRoleController);
        }
    }
}
