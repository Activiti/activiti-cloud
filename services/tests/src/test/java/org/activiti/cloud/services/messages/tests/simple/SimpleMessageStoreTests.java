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

package org.activiti.cloud.services.messages.tests.simple;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.messages.tests.AbstractMessagesCoreIntegrationTests;
import org.junit.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.integration.store.SimpleMessageStore;

@SpringBootConfiguration
@EnableAutoConfiguration
public class SimpleMessageStoreTests extends AbstractMessagesCoreIntegrationTests {
    
    @Test
    public void testMessageStore() throws Exception {
        assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(SimpleMessageStore.class);
    }
}
