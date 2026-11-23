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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
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
import org.springframework.integration.support.json.ErrorMessageJsonDeserializer;
import org.springframework.integration.support.json.GenericMessageJsonDeserializer;
import org.springframework.integration.support.json.MessageHeadersJsonSerializer;
import org.springframework.integration.support.json.MimeTypeJsonSerializer;
import org.springframework.integration.support.json.MutableMessageJsonDeserializer;
import org.springframework.integration.support.locks.DistributedLock;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ExceptionTypeFilter;
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
        final var errorMessageDeserializer = new ErrorMessageJsonDeserializer();

        final var messageModule = new SimpleModule("MessageModule")
            .addSerializer(new MessageHeadersJsonSerializer())
            .addSerializer(new MimeTypeJsonSerializer())
            .addDeserializer(GenericMessage.class, genericMessageDeserializer)
            .addDeserializer(MutableMessage.class, mutableMessageDeserializer)
            .addDeserializer(ErrorMessage.class, errorMessageDeserializer);

        final var basicPolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Message.class)
            .allowIfSubType(byte[].class)
            .allowIfSubTypeIsArray()
            .allowSubTypesWithExplicitDeserializer();

        Stream.concat(DEFAULT_TRUSTED_PACKAGES.stream(), Stream.of(trustedPackages)).forEach(
            basicPolymorphicTypeValidator::allowIfSubType
        );

        var jsonMapper = JsonMapper.builder()
            .activateDefaultTyping(
                basicPolymorphicTypeValidator.build(),
                DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
            )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .addModules(messageModule)
            .build();

        genericMessageDeserializer.setMapper(jsonMapper);
        mutableMessageDeserializer.setMapper(jsonMapper);
        errorMessageDeserializer.setMapper(jsonMapper);

        return () -> jsonMapper;
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
    OutboxIntegrationFlowFactory outboxIntegrationFlowFactory(
        OutboxProperties outboxProperties,
        PlatformTransactionManager transactionManager,
        ObjectProvider<StreamBridge> streamBridgeProvider,
        @OutboxLockRegistry LockRegistry<? extends Lock> lockRegistry,
        ExpressionEvaluatingRequestHandlerAdvice outboxMessageHandlerErrorAdvice
    ) {
        return new OutboxIntegrationFlowFactory(
            outboxProperties,
            transactionManager,
            streamBridgeProvider,
            lockRegistry,
            outboxMessageHandlerErrorAdvice
        );
    }

    @Bean
    OutboxChannelInterceptorFactory outboxChannelInterceptorFactory(
        OutboxProperties outboxProperties,
        @OutboxMessageStore ChannelMessageStore outboxChannelMessageStore,
        OutboxIntegrationFlowFactory outboxIntegrationFlowFactory,
        IntegrationFlowContext integrationFlowContext
    ) {
        return new OutboxChannelInterceptorFactory(
            outboxProperties,
            outboxChannelMessageStore,
            outboxIntegrationFlowFactory,
            integrationFlowContext
        );
    }

    @Bean
    public ExpressionEvaluatingRequestHandlerAdvice outboxMessageHandlerErrorAdvice(
        MessageChannel outboxFailureChannel
    ) {
        final var advice = new ExpressionEvaluatingRequestHandlerAdvice();
        // Route to this channel on failure
        advice.setFailureChannel(outboxFailureChannel);
        // Evaluate the root cause / original payload to pass to the error channel
        advice.setOnFailureExpressionString("#exception");
        // CRITICAL: Return true so the flow considers it "handled" and commits the transaction
        advice.setTrapException(true);

        return advice;
    }

    @Bean
    QueueChannel outboxDeadLetterQueue(ChannelMessageStore outboxChannelMessageStore) {
        return MessageChannels.queue(outboxChannelMessageStore, "outboxDeadLetterQueue").getObject();
    }

    @Bean
    public MessageChannel outboxFailureChannel() {
        return MessageChannels.direct().getObject();
    }

    @Bean
    public ExceptionTypeFilter outboxExceptionsTypeFilter(OutboxProperties outboxProperties) {
        final Set<Class<? extends Throwable>> includes = outboxProperties
            .getExceptions()
            .entrySet()
            .stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        final Collection<Class<? extends Throwable>> excludes = outboxProperties
            .getExceptions()
            .entrySet()
            .stream()
            .filter(Predicate.not(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        return new ExceptionTypeFilter(includes, excludes, true);
    }

    @Bean
    public IntegrationFlow outboxDeadLetterFlow(
        MessageChannel outboxFailureChannel,
        QueueChannel outboxDeadLetterQueue,
        ExceptionTypeFilter outboxExceptionsTypeFilter
    ) {
        return IntegrationFlow.from(outboxFailureChannel)
            .filter(
                ErrorMessage.class,
                message -> !outboxExceptionsTypeFilter.match(message.getPayload(), true),
                spec -> spec.throwExceptionOnRejection(true).discardWithinAdvice(true)
            )
            .transform(ErrorMessage.class, source -> {
                final var cause = NestedExceptionUtils.getMostSpecificCause(source.getPayload());

                if (source.getOriginalMessage() instanceof Message<?> originalMessage) {
                    return MessageBuilder.fromMessage(originalMessage)
                        .setHeader("exceptionMessage", cause.getMessage())
                        .build();
                } else if (source.getPayload() instanceof MessagingException messageException) {
                    return MessageBuilder.fromMessage(messageException.getFailedMessage())
                        .setHeader("exceptionMessage", cause.getMessage())
                        .build();
                }

                return null;
            })
            .channel(outboxDeadLetterQueue)
            .get();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxChannelConfigurer outboxChannelConfigurer() {
        return new OutboxChannelConfigurer();
    }

    @Bean
    NewDestinationBindingCallback<Object> outboxDestinationBindingCallback(
        OutboxChannelInterceptorFactory outboxChannelInterceptorFactory,
        OutboxDestinationProvider outboxDestinationProvider,
        OutboxChannelConfigurer outboxChannelConfigurer
    ) {
        final var channelInterceptor = outboxChannelInterceptorFactory.apply("streamBridge", outboxDestinationProvider);

        return new OutboxDestinationBindingCallback(channelInterceptor, outboxChannelConfigurer);
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
        OutboxDestinationProvider outboxDestinationProvider,
        OutboxChannelConfigurer outboxChannelConfigurer
    ) {
        return new OutboxOutputBindingPostProcessor(
            functionAnnotationService,
            outboxChannelInterceptorFactory,
            outboxDestinationProvider,
            outboxChannelConfigurer
        );
    }

    @Bean
    @OutboxMessageStore
    @ConditionalOnMissingBean(annotation = OutboxMessageStore.class)
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
    ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider(
        DataSource dataSource
    ) throws MetaDataAccessException {
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
        OutboxActuatorEndpoint outboxActuatorEndpoint(
            OutboxLifecycleController outboxLifecycleController,
            OutboxManager outboxManager
        ) {
            return new OutboxActuatorEndpoint(outboxLifecycleController, outboxManager);
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
    @ConditionalOnMissingBean(annotation = OutboxMetadataStore.class)
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

    @Bean
    @OutboxLockRepository
    @ConditionalOnMissingBean(annotation = OutboxLockRepository.class)
    public LockRepository outboxLockRepository(
        DataSource dataSource,
        PlatformTransactionManager transactionManager,
        @Value(
            "${activiti.cloud.messaging.outbox.lock.region:${spring.application.name:default}_outbox}}"
        ) String region
    ) {
        final var lockRepository = new DefaultLockRepository(dataSource);

        lockRepository.setRegion(region);
        lockRepository.setTransactionManager(transactionManager);
        lockRepository.setCheckDatabaseOnStart(true);

        return lockRepository;
    }

    @Bean
    @OutboxLockRegistry
    @ConditionalOnMissingBean(annotation = OutboxLockRegistry.class)
    LockRegistry<DistributedLock> outboxLockRegistry(@OutboxLockRepository LockRepository outboxLockRepository) {
        return new JdbcLockRegistry(outboxLockRepository);
    }
}
