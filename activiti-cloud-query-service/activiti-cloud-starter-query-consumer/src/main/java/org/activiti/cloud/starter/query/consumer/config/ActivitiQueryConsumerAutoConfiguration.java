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
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.support.json.JacksonJsonObjectMapper;
import org.springframework.integration.support.json.JacksonMessagingUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.ConstructorDetector;

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
        final var jsonObjectMapper = new JacksonJsonObjectMapper(
            JacksonMessagingUtils.messagingAwareMapper(trustedPackages)
                .rebuild()
                // Jackson 3 auto-promotes a single all-args constructor to a properties-based
                // creator. Event impls are serialized via their getters (e.g. the isPublic() getter
                // emits the property as "public"), so the creator parameter names never match the
                // JSON and every primitive arrives absent. EXPLICIT_ONLY keeps that auto-promotion
                // off, so deserialization falls back to the no-arg constructor + setters and the
                // values round-trip intact. See AAE-41740.
                .constructorDetector(ConstructorDetector.EXPLICIT_ONLY)
                // Outbox forward-compat: tolerate properties the consumer no longer knows about,
                // and keep absent primitives at their defaults rather than throwing.
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .build()
        );

        return () -> jsonObjectMapper;
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
