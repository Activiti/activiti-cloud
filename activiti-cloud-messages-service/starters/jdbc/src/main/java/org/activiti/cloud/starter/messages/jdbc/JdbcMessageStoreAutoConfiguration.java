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

package org.activiti.cloud.starter.messages.jdbc;

import javax.sql.DataSource;

import org.activiti.cloud.services.messages.core.config.MessageAggregatorProperties;
import org.activiti.cloud.services.messages.core.config.MessagesCoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnClass(JdbcMessageStore.class)
@AutoConfigureBefore({MessagesCoreAutoConfiguration.class})
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
@PropertySource("classpath:config/activiti-cloud-starter-messages-jdbc.properties")
public class JdbcMessageStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageGroupStore messageStore(JdbcTemplate jdbcTemplate, MessageAggregatorProperties properties) {
        JdbcMessageStore messageStore = new JdbcMessageStore(jdbcTemplate);
        messageStore.setLazyLoadMessageGroups(false);

        if (StringUtils.hasText(properties.getMessageStoreEntity())) {
            messageStore.setTablePrefix(properties.getMessageStoreEntity());
        }
        return messageStore;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConcurrentMetadataStore metadataStore(JdbcTemplate jdbcTemplate) {
        return new JdbcMetadataStore(jdbcTemplate);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LockRepository lockRepository(DataSource dataSource) {
        return new DefaultLockRepository(dataSource);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }        

}