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
package org.activiti.cloud.services.audit.jpa.cleanup.variable.transientpolicy.payload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableCreatedEventEntity;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditPayloadBatchWriterTest {

    private static final String SQL = "update audit_event set variable_instance = ? where id = ?";

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

    private AuditPayloadBatchWriter writer;

    @BeforeEach
    void createWriter() {
        writer = new AuditPayloadBatchWriter(entityManager);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void should_bindExtractedParametersInOrderAndExecuteOneBatch(int parameterCount) throws SQLException {
        connectWriter();
        var first = event(37L, "first");
        var second = event(91L, "second");
        List<Function<AuditEventEntity, ?>> extractors = List.of(
            AuditEventEntity::getEventId,
            AuditEventEntity::getId,
            event -> null
        );
        var parameters = extractors.subList(0, parameterCount);

        writer.update(SQL, List.of(first, second), parameters);

        var order = inOrder(statement);
        for (AuditEventEntity event : List.of(first, second)) {
            for (int index = 0; index < parameterCount; index++) {
                order.verify(statement).setObject(index + 1, parameters.get(index).apply(event));
            }
            order.verify(statement).addBatch();
        }
        order.verify(statement).executeBatch();
        order.verify(statement).close();
        then(statement).shouldHaveNoMoreInteractions();
        then(connection).should().prepareStatement(SQL);
        then(connection).shouldHaveNoMoreInteractions();
    }

    @Test
    void should_notAcquireConnectionForEmptyBatch() {
        writer.update(SQL, List.of(), List.of(AuditEventEntity::getId));

        verifyNoInteractions(entityManager, session, connection, statement);
    }

    @ParameterizedTest
    @ValueSource(strings = { "binding", "adding", "execution" })
    void should_propagateJdbcFailureAndCloseStatement(String failureStage) throws SQLException {
        connectWriter();
        var failure = new SQLException("batch failed");
        switch (failureStage) {
            case "binding" -> willThrow(failure).given(statement).setObject(anyInt(), any());
            case "adding" -> willThrow(failure).given(statement).addBatch();
            case "execution" -> given(statement.executeBatch()).willThrow(failure);
            default -> throw new IllegalArgumentException(failureStage);
        }

        assertThatThrownBy(() ->
            writer.update(SQL, List.of(event(37L, "first")), List.of(AuditEventEntity::getId))
        ).isSameAs(failure);

        then(statement).should().close();
        then(connection).should().prepareStatement(SQL);
        then(connection).shouldHaveNoMoreInteractions();
    }

    private void connectWriter() throws SQLException {
        given(entityManager.unwrap(Session.class)).willReturn(session);
        given(connection.prepareStatement(SQL)).willReturn(statement);
        willAnswer(invocation -> {
            invocation.getArgument(0, Work.class).execute(connection);
            return null;
        })
            .given(session)
            .doWork(any(Work.class));
    }

    private VariableCreatedEventEntity event(long id, String eventId) {
        var event = new VariableCreatedEventEntity();
        event.setId(id);
        event.setEventId(eventId);
        return event;
    }
}
