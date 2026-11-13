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
import static org.springframework.messaging.MessageHeaders.ERROR_CHANNEL;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.sql.DatabaseMetaData;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.activiti.cloud.common.messaging.config.FunctionAnnotationService;
import org.activiti.cloud.common.messaging.config.OutputBindingConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cloud.stream.binding.NewDestinationBindingCallback;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

@AutoConfiguration(after = OutputBindingConfiguration.class)
@ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.enabled")
@ConditionalOnClass({ DataSource.class, JdbcChannelMessageStore.class })
@EnableConfigurationProperties(OutboxProperties.class)
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
        OutboxProperties outboxProperties,
        ChannelMessageStore outboxChannelMessageStore,
        PlatformTransactionManager transactionManager,
        IntegrationFlowContext integrationFlowContext,
        ObjectProvider<StreamBridge> streamBridgeProvider,
        LockRegistry lockRegistry,
        ExpressionEvaluatingRequestHandlerAdvice outboxMessageHandlerErrorAdvice
    ) {
        return new OutboxChannelInterceptorFactory(
            outboxProperties,
            outboxChannelMessageStore,
            transactionManager,
            integrationFlowContext,
            streamBridgeProvider,
            lockRegistry,
            outboxMessageHandlerErrorAdvice
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpressionEvaluatingRequestHandlerAdvice outboxMessageHandlerErrorAdvice() {
        final var advice = new ExpressionEvaluatingRequestHandlerAdvice();
        // Route to this channel on failure
        advice.setFailureChannelName(ERROR_CHANNEL);
        // Evaluate the root cause / original payload to pass to the error channel
        advice.setOnFailureExpressionString("#exception");
        // CRITICAL: Return true so the flow considers it "handled" and commits the transaction
        advice.setTrapException(true);

        return advice;
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
    OutboxDestinationProvider outboxDestinationProvider(OutboxProperties outboxProperties) {
        return new OutboxDestinationProvider(outboxProperties);
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
        return new OutboxStreamBridgeOperationsPostProcessor();
    }

    @Bean
    JdbcChannelMessageStore outboxChannelMessageStore(
        DataSource dataSource,
        ChannelMessageStorePreparedStatementSetter channelMessageStorePreparedStatementSetter,
        MessageRowMapper messageRowMapper,
        ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider,
        @Value(
            "${activiti.cloud.messaging.outbox.store.region:${spring.application.name:default}_outbox}"
        ) String region
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
    OutboxLifecycleController outboxLifecycleController(
        SmartLifecycleRoleController smartLifecycleRoleController,
        ApplicationContext applicationContext
    ) {
        return new OutboxLifecycleController(smartLifecycleRoleController, applicationContext);
    }

    @Bean
    OutboxManager outboxManager(
        OutboxLifecycleController outboxLifecycleController,
        @OutboxMetadataStore JdbcMetadataStore outboxMetadataStore,
        OutboxLifecycleReconciler outboxLifecycleReconciler
    ) {
        return new OutboxManager(outboxLifecycleController, outboxMetadataStore, outboxLifecycleReconciler);
    }

    @Bean
    OutboxLifecycleReconciler outboxLifecycleReconciler(
        OutboxProperties outboxProperties,
        @OutboxMetadataStore JdbcMetadataStore outboxMetadataStore,
        OutboxLifecycleController outboxLifecycleController,
        TaskScheduler taskScheduler
    ) {
        return new OutboxLifecycleReconciler(
            outboxProperties,
            outboxMetadataStore,
            outboxLifecycleController,
            taskScheduler
        );
    }

    @Configuration
    @ConditionalOnClass({ Endpoint.class })
    @ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.actuator.enabled", matchIfMissing = true)
    static class OutboxActuatorConfiguration {

        @Bean
        OutboxActuatorEndpoint outboxActuatorEndpoint(OutboxLifecycleController outboxLifecycleController) {
            return new OutboxActuatorEndpoint(outboxLifecycleController);
        }
    }

    @Configuration
    @ConditionalOnClass({ HealthIndicator.class })
    @ConditionalOnProperty(name = "activiti.cloud.messaging.outbox.health.enabled", matchIfMissing = true)
    static class OutboxHealthIndicatorConfiguration {

        @Bean
        OutboxHealthIndicator outboxHealthIndicator(
            JdbcChannelMessageStore outboxQueueChannelMessageStore,
            OutboxLifecycleController outboxLifecycleController
        ) {
            return new OutboxHealthIndicator(outboxQueueChannelMessageStore, outboxLifecycleController);
        }
    }

    @Bean
    @OutboxMetadataStore
    public JdbcMetadataStore outboxMetadataStore(
        DataSource dataSource,
        @Value(
            "${activiti.cloud.messaging.outbox.metadata.region:${spring.application.name:default}_outbox}}"
        ) String region
    ) {
        JdbcMetadataStore metadataStore = new JdbcMetadataStore(dataSource);
        metadataStore.setRegion(region);
        return metadataStore;
    }
}
