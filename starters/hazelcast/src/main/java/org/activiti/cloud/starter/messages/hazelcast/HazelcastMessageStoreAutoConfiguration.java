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

package org.activiti.cloud.starter.messages.hazelcast;


import org.activiti.cloud.services.messages.core.config.MessagesCoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.data.geode.autoconfigure.ClientCacheAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.hazelcast.lock.HazelcastLockRegistry;
import org.springframework.integration.hazelcast.metadata.HazelcastMetadataStore;
import org.springframework.integration.hazelcast.store.HazelcastMessageStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.transaction.HazelcastTransactionManager;

@Configuration
@AutoConfigureBefore({MessagesCoreAutoConfiguration.class})
@ConditionalOnClass(HazelcastInstance.class)
public class HazelcastMessageStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Config hazelcastConfig() {
        Config config = new Config();
        
        config.getCPSubsystemConfig()
              .setCPMemberCount(3);
        
        return config;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastTransactionManager(hazelcastInstance);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageGroupStore messageStore(HazelcastInstance hazelcastInstance) {
        HazelcastMessageStore messageStore = new HazelcastMessageStore(hazelcastInstance);

        messageStore.setLazyLoadMessageGroups(false);
        
        return messageStore;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConcurrentMetadataStore metadataStore(HazelcastInstance hazelcastInstance) {
        return new HazelcastMetadataStore(hazelcastInstance);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockRegistry lockRegistry(HazelcastInstance hazelcastInstance) {
        return new HazelcastLockRegistry(hazelcastInstance);
    }    
}