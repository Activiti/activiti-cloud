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
package org.activiti.cloud.api.process.model.impl.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.ConnectorIncidentEvent;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.junit.jupiter.api.Test;

class ConnectorIncidentEventTest {

    @Test
    void should_createWithAllFields() {
        IntegrationContext context = mock(IntegrationContext.class);
        Exception exception = new RuntimeException("test error");

        ConnectorIncidentEvent event = new ConnectorIncidentEvent(context, exception, IncidentSeverity.WARNING);

        assertThat(event.getIntegrationContext()).isSameAs(context);
        assertThat(event.getException()).isSameAs(exception);
        assertThat(event.getSeverity()).isEqualTo(IncidentSeverity.WARNING);
    }

    @Test
    void should_createWithNoArgConstructor() {
        ConnectorIncidentEvent event = new ConnectorIncidentEvent();

        assertThat(event.getIntegrationContext()).isNull();
        assertThat(event.getException()).isNull();
        assertThat(event.getSeverity()).isNull();
    }

    @Test
    void should_supportSetters() {
        IntegrationContext context = mock(IntegrationContext.class);
        Exception exception = new RuntimeException("error");

        ConnectorIncidentEvent event = new ConnectorIncidentEvent();
        event.setIntegrationContext(context);
        event.setException(exception);
        event.setSeverity(IncidentSeverity.ERROR);

        assertThat(event.getIntegrationContext()).isSameAs(context);
        assertThat(event.getException()).isSameAs(exception);
        assertThat(event.getSeverity()).isEqualTo(IncidentSeverity.ERROR);
    }

    @Test
    void should_beEqual_whenSameFields() {
        IntegrationContext context = mock(IntegrationContext.class);
        Exception exception = new RuntimeException("error");

        ConnectorIncidentEvent event1 = new ConnectorIncidentEvent(context, exception, IncidentSeverity.WARNING);
        ConnectorIncidentEvent event2 = new ConnectorIncidentEvent(context, exception, IncidentSeverity.WARNING);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void should_notBeEqual_whenDifferentSeverity() {
        IntegrationContext context = mock(IntegrationContext.class);
        Exception exception = new RuntimeException("error");

        ConnectorIncidentEvent event1 = new ConnectorIncidentEvent(context, exception, IncidentSeverity.WARNING);
        ConnectorIncidentEvent event2 = new ConnectorIncidentEvent(context, exception, IncidentSeverity.ERROR);

        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void should_notBeEqual_whenDifferentException() {
        IntegrationContext context = mock(IntegrationContext.class);

        ConnectorIncidentEvent event1 = new ConnectorIncidentEvent(
            context,
            new RuntimeException("one"),
            IncidentSeverity.WARNING
        );
        ConnectorIncidentEvent event2 = new ConnectorIncidentEvent(
            context,
            new RuntimeException("two"),
            IncidentSeverity.WARNING
        );

        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void should_haveToString() {
        ConnectorIncidentEvent event = new ConnectorIncidentEvent(
            null,
            new RuntimeException("test"),
            IncidentSeverity.WARNING
        );

        assertThat(event.toString()).contains("WARNING").contains("test");
    }
}
