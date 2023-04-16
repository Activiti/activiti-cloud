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

package org.activiti.cloud.services.audit.jpa.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.impl.CloudIntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.IntegrationRequestSentEventEntity;
import org.junit.jupiter.api.Test;

class IntegrationRequestedEventConverterTest {

    private final IntegrationRequestedEventConverter integrationRequestedEventConverter =
            new IntegrationRequestedEventConverter(new EventContextInfoAppender());

    @Test
    void shouldConvertToAPIEvent() throws InterruptedException {
        CloudIntegrationContextImpl integrationContext = new CloudIntegrationContextImpl();
        CloudIntegrationRequestedEventImpl event = new CloudIntegrationRequestedEventImpl(integrationContext);
        event.setSequenceNumber(1);
        IntegrationRequestSentEventEntity eventEntity = new IntegrationRequestSentEventEntity(event);
        Thread.sleep(1);
        CloudRuntimeEventImpl<?, ?> apiEvent = integrationRequestedEventConverter.createAPIEvent(eventEntity);
        assertThat(apiEvent.getTimestamp()).isEqualTo(event.getTimestamp());
    }
}
