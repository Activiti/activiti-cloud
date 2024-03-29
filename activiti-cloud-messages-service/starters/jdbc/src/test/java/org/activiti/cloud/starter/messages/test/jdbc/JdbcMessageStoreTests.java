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
package org.activiti.cloud.starter.messages.test.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.tests.AbstractMessagesCoreIntegrationTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.test.context.TestPropertySource;

@Isolated
@TestPropertySource(properties = { "spring.sql.init.platform=h2" })
public class JdbcMessageStoreTests extends AbstractMessagesCoreIntegrationTests {

    @SpringBootApplication
    static class MessagesApplication {}

    @Test
    public void testMessageStore() throws Exception {
        assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(JdbcMessageStore.class);
    }
}
