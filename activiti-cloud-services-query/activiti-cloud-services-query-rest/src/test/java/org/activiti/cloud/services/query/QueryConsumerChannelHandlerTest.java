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

package org.activiti.cloud.services.query;

import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.runtime.api.event.impl.CloudProcessCreatedEventImpl;
import org.activiti.runtime.api.event.impl.CloudProcessStartedEventImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class QueryConsumerChannelHandlerTest {

    @InjectMocks
    private QueryConsumerChannelHandler consumer;

    @Mock
    private QueryEventHandlerContext eventHandlerContext;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void receiveShouldHandleReceivedEvent() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();

        //when
        consumer.receive(processCreatedEvent, processStartedEvent);

        //then
        verify(eventHandlerContext).handle(processCreatedEvent, processStartedEvent);
    }

}