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

package org.activiti.cloud.services.messages.core.router;

import static org.activiti.cloud.services.messages.core.integration.MessageEventHeaders.MESSAGE_EVENT_OUTPUT_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;

@ExtendWith(MockitoExtension.class)
public class CommandConsumerMessageRouterTest {

    @InjectMocks
    private CommandConsumerMessageRouter messageRouter;

    @Mock
    private DestinationResolver<MessageChannel> destinationResolver;

    @Test
    public void should_returnResultOfDestinationResolver_when_headerHasServiceFullName() {
        //given
        final String outputDestination = "messageConnectorOutput";
        final Message<String> message = MessageBuilder
            .withPayload("any")
            .setHeader(MESSAGE_EVENT_OUTPUT_DESTINATION, outputDestination)
            .build();

        final MessageChannel messageChannel = mock(MessageChannel.class);
        given(destinationResolver.resolveDestination(outputDestination)).willReturn(messageChannel);

        //when
        final Collection<MessageChannel> channels = messageRouter.determineTargetChannels(message);

        //then
        assertThat(channels).containsExactly(messageChannel);
    }

    @Test
    public void should_throwException_when_headerHasNotServiceFullName() {
        //given
        final Message<String> messageWithoutHeaders = MessageBuilder.withPayload("any").build();

        //then
        assertThatExceptionOfType(MessageMappingException.class)
            .isThrownBy(//when
            () -> messageRouter.determineTargetChannels(messageWithoutHeaders)
            )
            .withMessage("Unable to determine target channel for message");
    }

    @Test
    public void should_throwException_when_destinationResolverDoesNotFindADestination() {
        //given
        final String outputDestination = "messageConnectorOutput";
        final Message<String> message = MessageBuilder
            .withPayload("any")
            .setHeader(MESSAGE_EVENT_OUTPUT_DESTINATION, outputDestination)
            .build();

        given(destinationResolver.resolveDestination(outputDestination)).willReturn(null);

        //then
        assertThatExceptionOfType(MessageMappingException.class)
            .isThrownBy(//when
            () -> messageRouter.determineTargetChannels(message)
            )
            .withMessage("Unable to determine target channel for message");
    }
}
