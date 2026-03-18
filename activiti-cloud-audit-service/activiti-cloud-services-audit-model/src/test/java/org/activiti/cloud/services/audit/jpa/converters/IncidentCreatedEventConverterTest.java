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

import org.activiti.cloud.api.process.model.IncidentEvent.IncidentEventType;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.api.process.model.impl.IncidentContextImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.services.audit.jpa.events.IncidentCreatedEventEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentCreatedEventConverterTest {

    private IncidentCreatedEventConverter incidentCreatedEventConverter;

    @BeforeEach
    void setUp() {
        this.incidentCreatedEventConverter = new IncidentCreatedEventConverter(new EventContextInfoAppender());
    }

    @Test
    void verifySupportedEvent() {
        assertThat(this.incidentCreatedEventConverter.getSupportedEvent())
            .isEqualTo(IncidentEventType.INCIDENT_CREATED.name());
    }

    @Test
    void shouldCreateEventEntity() {
        var event = getIncidentCreatedEvent();
        event.setSequenceNumber(100);

        var entity = this.incidentCreatedEventConverter.createEventEntity(event);

        assertThat(entity.getSequenceNumber()).isEqualTo(100);
        assertThat(entity.getErrorMessage()).isEqualTo("Test Exception");
        assertThat(entity.getErrorClassName()).isEqualTo(IllegalArgumentException.class.getName());
        assertThat(entity.getIncidentContext()).isEqualTo(event.getEntity());
    }

    @Test
    void shouldCreateEventEntityWithDefaultSeverity() {
        var event = getIncidentCreatedEvent();

        var entity = this.incidentCreatedEventConverter.createEventEntity(event);

        assertThat(entity.getSeverity()).isEqualTo(IncidentSeverity.ERROR);
    }

    @Test
    void shouldCreateEventEntityWithExplicitSeverity() {
        var event = new CloudIncidentCreatedEventImpl(
            new IllegalArgumentException("Test Exception"),
            new IncidentContextImpl(),
            IncidentSeverity.WARNING
        );

        var entity = this.incidentCreatedEventConverter.createEventEntity(event);

        assertThat(entity.getSeverity()).isEqualTo(IncidentSeverity.WARNING);
    }

    @Test
    void shouldCreateAPIEvent() {
        var createdEvent = getIncidentCreatedEvent();
        createdEvent.setSequenceNumber(100);
        var entity = new IncidentCreatedEventEntity(createdEvent);

        var event = (CloudIncidentCreatedEventImpl) this.incidentCreatedEventConverter.createAPIEvent(entity);

        assertThat(event.getErrorMessage()).isEqualTo("Test Exception");
        assertThat(event.getErrorClassName()).isEqualTo(IllegalArgumentException.class.getName());
        assertThat(event.getEntity()).isEqualTo(entity.getIncidentContext());
    }

    @Test
    void shouldRoundTripDefaultSeverity() {
        var createdEvent = getIncidentCreatedEvent();
        var entity = new IncidentCreatedEventEntity(createdEvent);

        var event = (CloudIncidentCreatedEventImpl) this.incidentCreatedEventConverter.createAPIEvent(entity);

        assertThat(event.getSeverity()).isEqualTo(IncidentSeverity.ERROR);
    }

    @Test
    void shouldRoundTripExplicitWarningSeverity() {
        var createdEvent = new CloudIncidentCreatedEventImpl(
            new IllegalArgumentException("Test Exception"),
            new IncidentContextImpl(),
            IncidentSeverity.WARNING
        );
        var entity = new IncidentCreatedEventEntity(createdEvent);

        var event = (CloudIncidentCreatedEventImpl) this.incidentCreatedEventConverter.createAPIEvent(entity);

        assertThat(event.getSeverity()).isEqualTo(IncidentSeverity.WARNING);
    }

    private CloudIncidentCreatedEventImpl getIncidentCreatedEvent() {
        return new CloudIncidentCreatedEventImpl(
            new IllegalArgumentException("Test Exception"),
            new IncidentContextImpl()
        );
    }
}
