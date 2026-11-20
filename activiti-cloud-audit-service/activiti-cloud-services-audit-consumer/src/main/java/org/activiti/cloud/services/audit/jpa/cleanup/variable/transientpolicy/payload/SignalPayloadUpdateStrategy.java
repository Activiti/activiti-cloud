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

import jakarta.persistence.EntityManager;
import org.activiti.cloud.services.audit.jpa.converters.json.SignalJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.SignalReceivedAuditEventEntity;

public class SignalPayloadUpdateStrategy implements AuditPayloadUpdateStrategy {

    private final EntityManager entityManager;

    private final SignalJpaJsonConverter converter = new SignalJpaJsonConverter();

    public SignalPayloadUpdateStrategy(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean supports(AuditEventEntity event) {
        return event instanceof SignalReceivedAuditEventEntity;
    }

    @Override
    public int update(AuditEventEntity event) {
        var payloadEvent = (SignalReceivedAuditEventEntity) event;
        String payload = converter.convertToDatabaseColumn(payloadEvent.getSignal());

        return entityManager
            .createNativeQuery(
                """
                update audit_event
                set signal = :payload
                where id = :id
                """
            )
            .setParameter("payload", payload)
            .setParameter("id", event.getId())
            .executeUpdate();
    }
}
