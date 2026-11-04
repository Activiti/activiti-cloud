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

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;
import static org.springframework.integration.support.json.JacksonMessagingUtils.DEFAULT_TRUSTED_PACKAGES;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.sql.DatabaseMetaData;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.activiti.cloud.common.messaging.config.FunctionAnnotationService;
import org.activiti.cloud.common.messaging.config.OutputBindingConfiguration;
import org.activiti.cloud.common.messaging.config.StreamBridgeOperations;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
import org.springframework.context.annotation.Role;
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
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper;
import org.springframework.integration.support.json.GenericMessageJsonDeserializer;
import org.springframework.integration.support.json.MessageHeadersJsonSerializer;
import org.springframework.integration.support.json.MimeTypeJsonSerializer;
import org.springframework.integration.support.json.MutableMessageJsonDeserializer;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

@AutoConfiguration(after = OutputBindingConfiguration.class)
@ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.enabled")
@ConditionalOnClass({ DataSource.class, JdbcChannelMessageStore.class })
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Supplier<JsonMapper> outboxJsonObjectMapperProvider(
        @Value(
            "${activiti.cloud.messaging.outbox.json.trusted-packages:tools.jackson.databind.node,java.math,java.time}"
        ) String[] trustedPackages
    ) {
        final var genericMessageDeserializer = new GenericMessageJsonDeserializer();
        final var mutableMessageDeserializer = new MutableMessageJsonDeserializer();

        final var messageModule = new SimpleModule("MessageModule")
            .addSerializer(new MessageHeadersJsonSerializer())
            .addSerializer(new MimeTypeJsonSerializer())
            .addDeserializer(GenericMessage.class, genericMessageDeserializer)
            .addDeserializer(MutableMessage.class, mutableMessageDeserializer);

        final var basicPolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Message.class)
            .allowIfSubType(byte[].class)
            .allowIfSubTypeIsArray()
            .allowSubTypesWithExplicitDeserializer();

        Stream.concat(DEFAULT_TRUSTED_PACKAGES.stream(), Stream.of(trustedPackages)).forEach(
            basicPolymorphicTypeValidator::allowIfSubType
        );

        var mapper = JsonMapper.builder()
            .activateDefaultTyping(
                basicPolymorphicTypeValidator.build(),
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
            )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .addModules(messageModule)
            .build();

        genericMessageDeserializer.setMapper(mapper);
        mutableMessageDeserializer.setMapper(mapper);

        return () -> mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    EmbeddedHeadersJsonMessageMapper embeddedHeadersJsonMessageMapper(
        Supplier<JsonMapper> outboxJsonObjectMapperProvider
    ) {
        return new EmbeddedHeadersJsonMessageMapper(outboxJsonObjectMapperProvider.get(), "*");
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
        OutboxChannelInterceptorFactory outboxChannelInterceptorFactory,
        OutboxDestinationProvider outboxDestinationProvider
    ) {
        final var channelInterceptor = outboxChannelInterceptorFactory.apply("streamBridge", outboxDestinationProvider);

        return new OutboxDestinationBindingCallback(channelInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxDestinationProvider outboxDestinationProvider() {
        return new OutboxDestinationProvider();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    BeanPostProcessor outboxOutputBindingPostProcessor(
        FunctionAnnotationService functionAnnotationService,
        OutboxChannelInterceptorFactory outboxChannelInterceptorFactory,
        OutboxDestinationProvider outboxDestinationProvider
    ) {
        return new OutboxOutputBindingPostProcessor(
            functionAnnotationService,
            outboxChannelInterceptorFactory,
            outboxDestinationProvider
        );
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
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

        messageStore.setUsingIdCache(false);
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
