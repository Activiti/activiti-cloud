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
package org.activiti.cloud.starter.query.consumer.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.DatabaseMetaData;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.activiti.cloud.conf.FixedQueryConsumerPartitionedChannelCountProvider;
import org.activiti.cloud.conf.QueryConsumerAutoConfiguration;
import org.activiti.cloud.conf.QueryConsumerPartitionedChannelCountProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.H2ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.JsonChannelMessageStorePreparedStatementSetter;
import org.springframework.integration.jdbc.store.channel.JsonMessageRowMapper;
import org.springframework.integration.jdbc.store.channel.OracleChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.json.AdviceMessageJsonDeserializer;
import org.springframework.integration.support.json.ErrorMessageJsonDeserializer;
import org.springframework.integration.support.json.GenericMessageJsonDeserializer;
import org.springframework.integration.support.json.JacksonJsonObjectMapper;
import org.springframework.integration.support.json.JacksonMessagingUtils;
import org.springframework.integration.support.json.MutableMessageJsonDeserializer;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@AutoConfiguration(before = QueryConsumerAutoConfiguration.class, after = DataSourceAutoConfiguration.class)
@PropertySource("classpath:query-messaging.properties")
public class ActivitiQueryConsumerAutoConfiguration {

    private static final String POSTGRESQL = "postgresql";
    private static final String H2 = "h2";
    private static final String ORACLE = "oracle";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnBean(HikariDataSource.class)
    static class HikariDataSourceQueryConsumerPartitionedChannelConfiguration {

        @Bean
        @ConditionalOnMissingBean
        QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider(
            HikariDataSource dataSource
        ) {
            return new FixedQueryConsumerPartitionedChannelCountProvider(dataSource.getMaximumPoolSize());
        }
    }

    @Bean
    ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider(DataSource dataSource)
        throws MetaDataAccessException {
        final var databaseName = JdbcUtils.extractDatabaseMetaData(
            dataSource,
            DatabaseMetaData::getDatabaseProductName
        );

        return switch (databaseName.toLowerCase()) {
            case POSTGRESQL -> new PostgresChannelMessageStoreQueryProvider();
            case H2 -> new H2ChannelMessageStoreQueryProvider();
            case ORACLE -> new OracleChannelMessageStoreQueryProvider();
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseName);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    Supplier<JacksonJsonObjectMapper> queryConsumerJsonObjectMapperProvider(
        @Value(
            "${activiti.cloud.query.consumer.events.json.trusted-packages:org.activiti.api,org.activiti.cloud.api,java.math,java.time}"
        ) String[] trustedPackages
    ) {
        return () -> new JacksonJsonObjectMapper(outboxMessageMapper(trustedPackages));
    }

    /**
     * Builds the {@link JsonMapper} used to persist and read back {@code queryEvents} outbox messages.
     *
     * <p>{@link JacksonMessagingUtils#messagingAwareMapper} captures the mapper instance it builds inside
     * the message deserializers it registers (each {@code Message} payload is read with that captured
     * instance). Calling {@code .rebuild()} on the returned mapper produces a <em>new</em> mapper whose
     * deserializers still reference the original, so any configuration applied through {@code rebuild()}
     * never reaches the payload. We therefore re-register fresh message deserializers, build the mapper
     * with our settings, and wire that same instance back into them — so the configuration applies to the
     * payload as well as the envelope. See AAE-41740.
     *
     * <p>{@code EXPLICIT_ONLY} is the actual fix: Jackson 3 otherwise auto-promotes a single all-args
     * constructor to a properties-based creator, which looks up parameters by name (e.g. {@code isPublic})
     * while the JSON carries the getter-derived property name ({@code public}); the parameter then arrives
     * absent and a primitive blows up. {@code EXPLICIT_ONLY} keeps deserialization on the no-arg
     * constructor + setters path, so every event type round-trips without per-class annotations. The
     * {@code FAIL_ON_*} relaxations are retained as outbox forward-compatibility, not as the fix.
     */
    private static JsonMapper outboxMessageMapper(String[] trustedPackages) {
        final var genericMessageDeserializer = new GenericMessageJsonDeserializer();
        final var errorMessageDeserializer = new ErrorMessageJsonDeserializer();
        final var adviceMessageDeserializer = new AdviceMessageJsonDeserializer();
        final var mutableMessageDeserializer = new MutableMessageJsonDeserializer();

        final var messageModule = new SimpleModule()
            .addDeserializer(GenericMessage.class, genericMessageDeserializer)
            .addDeserializer(ErrorMessage.class, errorMessageDeserializer)
            .addDeserializer(AdviceMessage.class, adviceMessageDeserializer)
            .addDeserializer(MutableMessage.class, mutableMessageDeserializer);

        final var mapper = JacksonMessagingUtils.messagingAwareMapper(trustedPackages)
            .rebuild()
            .constructorDetector(ConstructorDetector.EXPLICIT_ONLY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .addModules(messageModule)
            .build();

        genericMessageDeserializer.setMapper(mapper);
        errorMessageDeserializer.setMapper(mapper);
        adviceMessageDeserializer.setMapper(mapper);
        mutableMessageDeserializer.setMapper(mapper);

        return mapper;
    }

    @Bean
    ChannelMessageStore queryEventsChannelMessageStore(
        DataSource dataSource,
        ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider,
        Supplier<JacksonJsonObjectMapper> queryConsumerJsonObjectMapperProvider,
        @Value("${activiti.cloud.messaging.instance-index:0}") Integer instanceIndex
    ) {
        final var messageStore = new JdbcChannelMessageStore(dataSource);
        final var jsonObjectMapper = queryConsumerJsonObjectMapperProvider.get();

        messageStore.setTablePrefix("QUERY_INT_");
        messageStore.setChannelMessageStoreQueryProvider(channelMessageStoreQueryProvider);
        messageStore.setCheckDatabaseOnStart(true);
        messageStore.setPreparedStatementSetter(new JsonChannelMessageStorePreparedStatementSetter(jsonObjectMapper));
        messageStore.setMessageRowMapper(new JsonMessageRowMapper(jsonObjectMapper));
        messageStore.setRegion("query-events-".concat(String.valueOf(instanceIndex)));

        return messageStore;
    }
}
