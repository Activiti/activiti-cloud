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
import org.activiti.cloud.conf.FixedQueryConsumerPartitionedChannelCountProvider;
import org.activiti.cloud.conf.QueryConsumerAutoConfiguration;
import org.activiti.cloud.conf.QueryConsumerPartitionedChannelCountProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration(before = QueryConsumerAutoConfiguration.class)
@PropertySource("classpath:query-messaging.properties")
public class ActivitiQueryConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(HikariDataSource.class)
    QueryConsumerPartitionedChannelCountProvider queryConsumerPartitionedChannelCountProvider(
        HikariDataSource dataSource
    ) {
        return new FixedQueryConsumerPartitionedChannelCountProvider(dataSource.getMaximumPoolSize() * 2);
    }
}
