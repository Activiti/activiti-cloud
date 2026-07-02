/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import java.util.Map;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.impl.CloudIntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.IntegrationResultReceivedEventEntity;
import org.junit.jupiter.api.Test;

class IntegrationResultReceivedEventConverterTest {

    private final IntegrationResultReceivedEventConverter integrationResultReceivedEventConverter =
        new IntegrationResultReceivedEventConverter(new EventContextInfoAppender());

    @Test
    void createEventEntity_should_clearInBoundVariables() {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.addInBoundVariables(Map.of("inputVar", "inputValue"));
        integrationContext.addOutBoundVariables(Map.of("outputVar", "outputValue"));

        CloudIntegrationResultReceivedEventImpl resultReceivedEvent = new CloudIntegrationResultReceivedEventImpl(
            integrationContext
        );
        resultReceivedEvent.setSequenceNumber(1);

        //when
        IntegrationResultReceivedEventEntity resultEntity = integrationResultReceivedEventConverter.createEventEntity(
            resultReceivedEvent
        );

        //then
        assertThat(resultEntity.getIntegrationContext()).isNotNull();
        assertThat(resultEntity.getIntegrationContext().getInBoundVariables()).isEmpty();
        assertThat(resultEntity.getIntegrationContext().getOutBoundVariables()).containsExactlyInAnyOrderEntriesOf(
            Map.of("outputVar", "outputValue")
        );
    }

    @Test
    void shouldConvertToAPIEvent() throws InterruptedException {
        CloudIntegrationContextImpl integrationContext = new CloudIntegrationContextImpl();
        CloudIntegrationResultReceivedEventImpl event = new CloudIntegrationResultReceivedEventImpl(integrationContext);
        event.setSequenceNumber(1);
        IntegrationResultReceivedEventEntity eventEntity = new IntegrationResultReceivedEventEntity(event);
        eventEntity.setEventId("eventId");
        Thread.sleep(1); // sleep to make sure the timestamp is retrieved from the db and is not current time
        CloudRuntimeEventImpl<?, ?> apiEvent = integrationResultReceivedEventConverter.createAPIEvent(eventEntity);
        assertThat(apiEvent.getTimestamp()).isEqualTo(event.getTimestamp());
    }
}
