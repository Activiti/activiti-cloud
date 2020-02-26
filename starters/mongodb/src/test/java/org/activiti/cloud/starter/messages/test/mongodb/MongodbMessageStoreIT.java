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

package org.activiti.cloud.starter.messages.test.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.tests.AbstractMessagesCoreIntegrationTests;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.mongodb.store.ConfigurableMongoDbMessageStore;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test?maxPoolSize=100&minPoolSize=10"})
public class MongodbMessageStoreIT extends AbstractMessagesCoreIntegrationTests {

    @SpringBootApplication
    static class MessagesApplication {
        
    }
    
    @Test
    public void testMessageStore() throws Exception {
        assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(ConfigurableMongoDbMessageStore.class);
    }
} 