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
package org.activiti.cloud.starter.messages.test.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.tests.AbstractMessagesCoreIntegrationTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.redis.store.RedisMessageStore;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

@Isolated
@ContextConfiguration(initializers = RedisApplicationInitializer.class)
public class RedisMessageStoreIT extends AbstractMessagesCoreIntegrationTests {

    @SpringBootApplication
    static class MessagesApplication {}

    @TestConfiguration
    static class Configuration {

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new PseudoTransactionManager();
        }
    }

    @Test
    public void testMessageStore() {
        assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(RedisMessageStore.class);
    }
}
