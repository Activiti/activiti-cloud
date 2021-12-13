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
package org.activiti.cloud.conf;

import static org.mockito.Mockito.verify;
import java.util.List;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({ TestChannelBinderConfiguration.class })
@ActiveProfiles("binding")
class QueryConsumerChannelConfigurationTest {

    @Autowired
    private InputDestination inputDestination;

    @MockBean
    private QueryEventHandlerContext eventHandlerContext;

    @SpringBootApplication
    static class Application {
    }

    @Test
    public void shouldHaveChannelBindingsSetForQueryConsumer() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();

        //when
        inputDestination.send(new GenericMessage<>(List.of(processCreatedEvent, processStartedEvent)));

        //then
        verify(eventHandlerContext).handle(processCreatedEvent, processStartedEvent);
    }
}