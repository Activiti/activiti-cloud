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

package org.activiti.cloud.starter.messages.redis;


import org.activiti.cloud.services.messages.core.config.MessagesCoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
//import org.springframework.boot.data.geode.autoconfigure.ClientCacheAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.redis.store.RedisMessageStore;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;

@Configuration
@ConditionalOnClass(RedisMessageStore.class)
@AutoConfigureBefore({MessagesCoreAutoConfiguration.class})
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisMessageStoreAutoConfiguration {

    @Autowired
    public void configure(RedisTemplate<Object, Object> redisTemplate) {
        redisTemplate.setEnableTransactionSupport(true);
    }
    
    @Bean
    public MessageGroupStore messageStore(RedisTemplate<?, ?> redisTemplate) {
        RedisMessageStore messageStore = new RedisMessageStore(redisTemplate.getConnectionFactory());
        messageStore.setLazyLoadMessageGroups(false);
        
        return messageStore;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConcurrentMetadataStore metadataStore(RedisConnectionFactory connectionFactory) {
        return new RedisMetadataStore(connectionFactory);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, "RedisLockRegistry");
    }
    
}