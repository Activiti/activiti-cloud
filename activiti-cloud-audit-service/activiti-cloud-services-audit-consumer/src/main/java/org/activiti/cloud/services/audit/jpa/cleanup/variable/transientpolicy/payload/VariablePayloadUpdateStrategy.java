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
import org.activiti.cloud.services.audit.jpa.converters.json.VariableJpaJsonConverter;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.VariableAuditEventEntity;

public class VariablePayloadUpdateStrategy implements AuditPayloadUpdateStrategy {

    private final EntityManager entityManager;

    private final VariableJpaJsonConverter converter = new VariableJpaJsonConverter();

    public VariablePayloadUpdateStrategy(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean supports(AuditEventEntity event) {
        return event instanceof VariableAuditEventEntity;
    }

    @Override
    public int update(AuditEventEntity event) {
        var payloadEvent = (VariableAuditEventEntity) event;
        String payload = converter.convertToDatabaseColumn(payloadEvent.getVariableInstance());

        return entityManager
            .createNativeQuery(
                """
                update audit_event
                set variable_instance = :payload,
                    variable_previous_value = null
                where id = :id
                """
            )
            .setParameter("payload", payload)
            .setParameter("id", event.getId())
            .executeUpdate();
    }
}
