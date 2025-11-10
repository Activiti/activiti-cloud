/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.events.message;

import static org.activiti.cloud.services.events.TestUtils.MOCK_APP_VERSION;
import static org.activiti.cloud.services.events.TestUtils.MOCK_BUSINESS_KEY;
import static org.activiti.cloud.services.events.TestUtils.MOCK_DEPLOYMENT_ID;
import static org.activiti.cloud.services.events.TestUtils.MOCK_DEPLOYMENT_NAME;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PARENT_PROCESS_INSTANCE_ID;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PARENT_PROCESS_NAME;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PROCESS_DEFINITION_ID;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PROCESS_DEFINITION_KEY;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PROCESS_DEFINITION_NAME;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PROCESS_DEFINITION_VERSION;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PROCESS_INSTANCE_ID;
import static org.activiti.cloud.services.events.TestUtils.MOCK_PROCESS_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.conf.IgnoredRuntimeEvent;
import org.activiti.cloud.services.events.TestUtils;
import org.activiti.engine.impl.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class ExecutionContextMessageBuilderAppenderTest {

    private ExecutionContextMessageBuilderAppender subject;

    @BeforeEach
    public void setUp() {
        ExecutionContext context = TestUtils.mockExecutionContext();

        subject = new ExecutionContextMessageBuilderAppender(context);
    }

    @Test
    public void testApply() {
        // given
        MessageBuilder<CloudRuntimeEvent<?, ?>> request = MessageBuilder.withPayload(new IgnoredRuntimeEvent());

        // when
        subject.apply(request);

        // then
        Message<CloudRuntimeEvent<?, ?>> message = request.build();

        assertThat(message.getHeaders())
            .containsEntry(ExecutionContextMessageHeaders.ROOT_BUSINESS_KEY, MOCK_BUSINESS_KEY)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_INSTANCE_ID, MOCK_PROCESS_INSTANCE_ID)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_ID, MOCK_PROCESS_DEFINITION_ID)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_KEY, MOCK_PROCESS_DEFINITION_KEY)
            .containsEntry(ExecutionContextMessageHeaders.PARENT_PROCESS_INSTANCE_ID, MOCK_PARENT_PROCESS_INSTANCE_ID)
            .containsEntry(
                ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_VERSION,
                MOCK_PROCESS_DEFINITION_VERSION
            )
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_NAME, MOCK_PROCESS_NAME)
            .containsEntry(ExecutionContextMessageHeaders.PARENT_PROCESS_INSTANCE_NAME, MOCK_PARENT_PROCESS_NAME)
            .containsEntry(ExecutionContextMessageHeaders.ROOT_PROCESS_DEFINITION_NAME, MOCK_PROCESS_DEFINITION_NAME)
            .containsEntry(ExecutionContextMessageHeaders.DEPLOYMENT_ID, MOCK_DEPLOYMENT_ID)
            .containsEntry(ExecutionContextMessageHeaders.DEPLOYMENT_NAME, MOCK_DEPLOYMENT_NAME)
            .containsEntry(ExecutionContextMessageHeaders.DEPLOYMENT_VERSION, MOCK_APP_VERSION);
    }
}
