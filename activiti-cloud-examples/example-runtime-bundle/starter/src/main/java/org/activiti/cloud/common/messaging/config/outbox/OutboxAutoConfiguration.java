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
package org.activiti.cloud.common.messaging.config.outbox;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.activiti.cloud.common.messaging.config.OutputBindingConfiguration;
import org.activiti.cloud.services.messages.core.config.MessagesCoreAutoConfiguration;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binding.NewDestinationBindingCallback;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.JsonChannelMessageStorePreparedStatementSetter;
import org.springframework.integration.jdbc.store.channel.JsonMessageRowMapper;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.support.json.AdviceMessageJsonDeserializer;
import org.springframework.integration.support.json.ErrorMessageJsonDeserializer;
import org.springframework.integration.support.json.GenericMessageJsonDeserializer;
import org.springframework.integration.support.json.JacksonJsonObjectMapper;
import org.springframework.integration.support.json.MessageHeadersJsonSerializer;
import org.springframework.integration.support.json.MimeTypeJsonSerializer;
import org.springframework.integration.support.json.MutableMessageJsonDeserializer;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;
import tools.jackson.databind.module.SimpleModule;

@AutoConfiguration(before = OutputBindingConfiguration.class, after = MessagesCoreAutoConfiguration.class)
@ConditionalOnProperty(name = "alresco.process.runtime.outbox.enabled", matchIfMissing = true)
public class OutboxAutoConfiguration {

    @Bean
    @Primary
    StreamOperations outboxStreamBridge(ObjectProvider<StreamBridge> streamBridgeProvider) {
        return new OutboxStreamBridge(streamBridgeProvider);
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
    BeanPostProcessor outboxStreamBridgePostProcessor(OutboxChannelInterceptorFactory outboxChannelInterceptorFactory) {
        return new BeanPostProcessor() {
            @Override
            public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof OutboxStreamBridge) {
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
    Supplier<List<JacksonModule>> trustedOutboxJacksonModulesProvider(
        List<JacksonModule> jacksonModules,
        @Value("${alresco.process.runtime.outbox.json.trusted-modules:^map.*}") Pattern[] trustedModules
    ) {
        return () ->
            jacksonModules
                .stream()
                .filter(it ->
                    Stream.of(trustedModules).anyMatch(pattern -> pattern.matcher(it.getModuleName()).matches())
                )
                .toList();
    }

    @Bean
    @ConditionalOnMissingBean
    Supplier<JacksonJsonObjectMapper> outboxJsonObjectMapperProvider(
        @Value(
            "${alresco.process.runtime.outbox.json.trusted-packages:org.activiti.api,org.activiti.cloud.api,com.alfresco,tools.jackson.databind.node,java.math,java.time}"
        ) String[] trustedPackages,
        Supplier<List<JacksonModule>> trustedOutboxJacksonModulesProvider
    ) {
        final var genericMessageDeserializer = new GenericMessageJsonDeserializer();
        final var errorMessageDeserializer = new ErrorMessageJsonDeserializer();
        final var adviceMessageDeserializer = new AdviceMessageJsonDeserializer();
        final var mutableMessageDeserializer = new MutableMessageJsonDeserializer();

        final var messageModule = new SimpleModule("MessageModule")
            .addSerializer(new MessageHeadersJsonSerializer())
            .addSerializer(new MimeTypeJsonSerializer())
            .addDeserializer(GenericMessage.class, genericMessageDeserializer)
            .addDeserializer(ErrorMessage.class, errorMessageDeserializer)
            .addDeserializer(AdviceMessage.class, adviceMessageDeserializer)
            .addDeserializer(MutableMessage.class, mutableMessageDeserializer);

        JsonMapper mapper = JsonMapper.builder()
            .setDefaultTyping(
                new DefaultTypeResolverBuilder(
                    BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                    DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
                )
            )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
            .addModules(trustedOutboxJacksonModulesProvider.get())
            .addModules(messageModule)
            .build();

        genericMessageDeserializer.setMapper(mapper);
        errorMessageDeserializer.setMapper(mapper);
        adviceMessageDeserializer.setMapper(mapper);
        mutableMessageDeserializer.setMapper(mapper);

        return () -> new JacksonJsonObjectMapper(mapper);
    }

    @Bean
    JdbcChannelMessageStore outboxChannelMessageStore(
        DataSource dataSource,
        Supplier<JacksonJsonObjectMapper> outboxJsonObjectMapperProvider,
        ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider
    ) {
        final var messageStore = new JdbcChannelMessageStore(dataSource);
        final var jsonObjectMapper = outboxJsonObjectMapperProvider.get();

        messageStore.setUsingIdCache(true);
        messageStore.setChannelMessageStoreQueryProvider(channelMessageStoreQueryProvider);
        messageStore.setCheckDatabaseOnStart(true);
        messageStore.setPreparedStatementSetter(new JsonChannelMessageStorePreparedStatementSetter(jsonObjectMapper));
        messageStore.setMessageRowMapper(new JsonMessageRowMapper(jsonObjectMapper));
        messageStore.setRegion("rb");

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
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseName);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "alresco.process.runtime.outbox.health.enabled", matchIfMissing = true)
    OutboxHealthIndicator outboxHealthIndicator(
        JdbcChannelMessageStore outboxQueueChannelMessageStore,
        SmartLifecycleRoleController smartLifecycleRoleController
    ) {
        return new OutboxHealthIndicator(outboxQueueChannelMessageStore, smartLifecycleRoleController);
    }

    @Bean
    OutboxManager outboxManager(SmartLifecycleRoleController smartLifecycleRoleController) {
        return new OutboxManager(smartLifecycleRoleController);
    }

    @Bean
    @ConditionalOnProperty(name = "alresco.process.runtime.outbox.actuator.enabled", matchIfMissing = true)
    OutboxActuatorEndpoint outboxActuatorEndpoint(SmartLifecycleRoleController smartLifecycleRoleController) {
        return new OutboxActuatorEndpoint(smartLifecycleRoleController);
    }
}
