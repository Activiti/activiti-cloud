/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.starter.messages.test.hazelcast;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.tests.AbstractMessagesCoreIntegrationTests;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.hazelcast.store.HazelcastMessageStore;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Disabled
public class HazelcastMessageStoreIT extends AbstractMessagesCoreIntegrationTests {

    @SpringBootApplication
    static class MessagesApplication {

    }

    @TestConfiguration
    static class HazelcastConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public Config hazelcastConfig() {
            Config config = new Config();

            config.getCPSubsystemConfig()
                  .setCPMemberCount(3);

            NetworkConfig network = config.getNetworkConfig()
                                          .setPortAutoIncrement(true);
            network.setPort(5701)
                   .setPortCount(20);

            JoinConfig join = network.getJoin();

            join.getMulticastConfig()
                .setEnabled(false);

            join.getTcpIpConfig()
                .setEnabled(true)
                .addMember("localhost");

            return config;
        }

        @Bean(destroyMethod = "shutdown")
        public HazelcastInstance hazelcastInstance(Config hazelcastConfig) {
            hazelcastConfig.getNetworkConfig()
                           .setPublicAddress("localhost:5701");

            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        }

        @Bean(destroyMethod = "shutdown")
        public HazelcastInstance hazelcastInstance2(Config hazelcastConfig) {
            hazelcastConfig.getNetworkConfig()
                           .setPublicAddress("localhost:5702");

            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        }

        @Bean(destroyMethod = "shutdown")
        public HazelcastInstance hazelcastInstance3(Config hazelcastConfig) {
            hazelcastConfig.getNetworkConfig()
                           .setPublicAddress("localhost:5703");

            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        }
    }

    @Test
    public void testMessageStore() throws Exception {
        assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(HazelcastMessageStore.class);
    }
}
