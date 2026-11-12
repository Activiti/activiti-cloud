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
package org.activiti.cloud.services.query.events.handlers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceLinkHierarchyListenerTest {

    private static final String[] PROPERTY_NAMES = { "name", "linkedProcessInstanceId" };

    private final ProcessInstanceLinkHierarchyListener listener = new ProcessInstanceLinkHierarchyListener();

    @Mock
    private PostUpdateEvent event;

    @Mock
    private EntityPersister persister;

    @Mock
    private EventSource session;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

    private ProcessInstanceEntity processInstance;

    @BeforeEach
    void setUp() {
        processInstance = new ProcessInstanceEntity();
        processInstance.setId("orphan");
    }

    @Test
    void should_registerLinkedRows_when_linkedProcessInstanceIdChangesFromNullToValue() throws SQLException {
        givenUpdate(new Object[] { "name", null }, new Object[] { "name", "main" });
        given(event.getSession()).willReturn(session);
        given(connection.prepareStatement(anyString())).willReturn(statement);

        listener.onPostUpdate(event);

        executeCapturedWork();
        verify(statement).setString(1, "main");
        verify(statement).setString(2, "orphan");
        verify(statement).executeUpdate();
    }

    @Test
    void should_notRegisterLinkedRows_when_linkedProcessInstanceIdWasAlreadySet() {
        givenUpdate(new Object[] { "name", "main" }, new Object[] { "name", "otherMain" });

        listener.onPostUpdate(event);

        verifyNoInteractions(session);
    }

    @Test
    void should_notRegisterLinkedRows_when_linkedProcessInstanceIdIsNotChanged() {
        givenUpdate(new Object[] { "name", null }, new Object[] { "renamed", null });

        listener.onPostUpdate(event);

        verifyNoInteractions(session);
    }

    @Test
    void should_notRegisterLinkedRows_when_oldStateIsNotAvailable() {
        given(event.getEntity()).willReturn(processInstance);
        given(event.getOldState()).willReturn(null);

        listener.onPostUpdate(event);

        verifyNoInteractions(session);
    }

    @Test
    void should_notRegisterLinkedRows_when_entityIsNotProcessInstance() {
        given(event.getEntity()).willReturn(new Object());

        listener.onPostUpdate(event);

        verify(event, never()).getOldState();
    }

    private void givenUpdate(Object[] oldState, Object[] state) {
        given(event.getEntity()).willReturn(processInstance);
        given(event.getPersister()).willReturn(persister);
        given(persister.getPropertyNames()).willReturn(PROPERTY_NAMES);
        given(event.getOldState()).willReturn(oldState);
        lenient().when(event.getState()).thenReturn(state);
    }

    private void executeCapturedWork() throws SQLException {
        ArgumentCaptor<Work> work = ArgumentCaptor.forClass(Work.class);
        verify(session).doWork(work.capture());
        work.getValue().execute(connection);
    }
}
