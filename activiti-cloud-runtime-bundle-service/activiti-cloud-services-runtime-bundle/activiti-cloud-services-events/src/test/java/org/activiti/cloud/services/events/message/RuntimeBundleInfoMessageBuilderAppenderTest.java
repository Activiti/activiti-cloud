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
package org.activiti.cloud.services.events.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.conf.IgnoredRuntimeEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class RuntimeBundleInfoMessageBuilderAppenderTest {

    private static final String SPRING_APP_NAME = "springAppName";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String APP_NAME = "appName";

    private RuntimeBundleInfoMessageBuilderAppender subject;

    @BeforeEach
    public void setUp() {
        RuntimeBundleProperties properties = new RuntimeBundleProperties();

        properties.setAppName(APP_NAME);
        properties.setServiceType(SERVICE_TYPE);
        properties.setServiceVersion(SERVICE_VERSION);
        properties.setRbSpringAppName(SPRING_APP_NAME);

        subject = new RuntimeBundleInfoMessageBuilderAppender(properties);
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
            .containsEntry(RuntimeBundleInfoMessageHeaders.APP_NAME, APP_NAME)
            .containsEntry(RuntimeBundleInfoMessageHeaders.SERVICE_NAME, SPRING_APP_NAME)
            .containsEntry(RuntimeBundleInfoMessageHeaders.SERVICE_TYPE, SERVICE_TYPE)
            .containsEntry(RuntimeBundleInfoMessageHeaders.SERVICE_VERSION, SERVICE_VERSION);
    }
}
