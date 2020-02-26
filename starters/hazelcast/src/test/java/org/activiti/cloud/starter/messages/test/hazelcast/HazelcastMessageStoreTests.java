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

package org.activiti.cloud.starter.messages.test.hazelcast;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.tests.AbstractMessagesCoreIntegrationTests;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.hazelcast.store.HazelcastMessageStore;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastMessageStoreTests extends AbstractMessagesCoreIntegrationTests {
    
    @SpringBootApplication
    static class MessagesApplication {
        
    }
    
    @TestConfiguration
    static class HazelcastConfiguration {
        
        @Bean
        public HazelcastInstance hazelcastInstance(Config hazelcastConfig) {
            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        }

        @Bean
        public HazelcastInstance hazelcastInstance2(Config hazelcastConfig) {
            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        }

        @Bean
        public HazelcastInstance hazelcastInstance3(Config hazelcastConfig) {
            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        }
    }

    @Test
    public void testMessageStore() throws Exception {
        assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(HazelcastMessageStore.class);
    }
}
