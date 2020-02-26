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

package org.activiti.cloud.starter.messages.mongodb;


import java.util.Arrays;

import org.activiti.cloud.services.messages.core.config.MessageAggregatorProperties;
import org.activiti.cloud.services.messages.core.config.MessagesCoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
//import org.springframework.boot.data.geode.autoconfigure.ClientCacheAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.mongodb.metadata.MongoDbMetadataStore;
import org.springframework.integration.mongodb.store.ConfigurableMongoDbMessageStore;
import org.springframework.integration.mongodb.support.BinaryToMessageConverter;
import org.springframework.integration.mongodb.support.MessageToBinaryConverter;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.StringUtils;


/**
 * A helper class containing configuration classes for particular technologies
 * to expose an appropriate {@link org.springframework.integration.store.MessageStore} bean
 * via matched configuration properties.
 *
 */
@Configuration
@ConditionalOnClass(ConfigurableMongoDbMessageStore.class)
@AutoConfigureBefore({MessagesCoreAutoConfiguration.class})
@AutoConfigureAfter({
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
public class MongoDbMessageStoreAutoConfiguration {

    @Bean
    public MessageGroupStore messageStore(MongoTemplate mongoTemplate, MessageAggregatorProperties properties) {
        ConfigurableMongoDbMessageStore messageStore;
        
        if (StringUtils.hasText(properties.getMessageStoreEntity())) {
            messageStore = new ConfigurableMongoDbMessageStore(mongoTemplate, properties.getMessageStoreEntity());
        }
        else {
            messageStore = new ConfigurableMongoDbMessageStore(mongoTemplate);
        }
        
        messageStore.setLazyLoadMessageGroups(false);
        
        return messageStore;
    }

    @Bean
    @Primary
    public MongoCustomConversions mongoDbCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new MessageToBinaryConverter(), new BinaryToMessageConverter()));
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MongoTransactionManager transactionManager(MongoDbFactory mongoDbFactory) {
        return new MongoTransactionManager(mongoDbFactory);
    }        
    
    @Bean
    @ConditionalOnMissingBean
    public ConcurrentMetadataStore metadataStore(MongoTemplate mongoTemplate) {
        return new MongoDbMetadataStore(mongoTemplate);
    }
            
    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry() {
        return new DefaultLockRegistry();
    }

}