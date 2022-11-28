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
package org.activiti.cloud.services.events.tests.util;

import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

@Configuration
public class MockProcessEngineChannels implements ProcessEngineChannels {

    @Override
    public SubscribableChannel commandConsumer() {
        return null;
    }

    @Override
    public MessageChannel commandResults() {
        return null;
    }

    @Override
    public MessageChannel auditProducer() {
        return new MockMessageChannel();
    }


}