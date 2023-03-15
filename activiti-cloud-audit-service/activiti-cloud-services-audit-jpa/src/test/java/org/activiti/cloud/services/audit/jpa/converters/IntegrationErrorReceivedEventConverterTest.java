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

import java.util.Collections;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.IntegrationErrorReceivedEventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IntegrationErrorReceivedEventConverterTest {

    @InjectMocks
    private IntegrationErrorReceivedEventConverter converter;

    @Test
    public void createEventEntity_should_setErrorRelatedProperties() {
        //given
        CloudIntegrationErrorReceivedEventImpl errorReceivedEvent = new CloudIntegrationErrorReceivedEventImpl(
            new IntegrationContextImpl(),
            "errorCode",
            "Something went wrong",
            RuntimeException.class.getName(),
            Collections.singletonList(new StackTraceElement("any", "any", "any", 1))
        );
        errorReceivedEvent.setSequenceNumber(1);

        //when
        IntegrationErrorReceivedEventEntity errorReceivedEventEntity = converter.createEventEntity(errorReceivedEvent);

        //then
        assertThat(errorReceivedEventEntity.getIntegrationContext()).isEqualTo(errorReceivedEvent.getEntity());
        assertThat(errorReceivedEventEntity.getErrorCode()).isEqualTo(errorReceivedEvent.getErrorCode());
        assertThat(errorReceivedEventEntity.getErrorMessage()).isEqualTo(errorReceivedEventEntity.getErrorMessage());
        assertThat(errorReceivedEventEntity.getErrorClassName())
            .isEqualTo(errorReceivedEventEntity.getErrorClassName());
        assertThat(errorReceivedEventEntity.getStackTraceElements())
            .isEqualTo(errorReceivedEventEntity.getStackTraceElements());
    }
}
