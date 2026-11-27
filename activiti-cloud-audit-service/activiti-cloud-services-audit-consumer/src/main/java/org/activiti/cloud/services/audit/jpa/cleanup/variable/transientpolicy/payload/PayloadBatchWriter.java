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
import java.util.List;
import java.util.function.Function;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.hibernate.Session;

public class PayloadBatchWriter {

    private final EntityManager entityManager;

    public PayloadBatchWriter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void update(
        String sql,
        List<AuditEventEntity> events,
        List<Function<AuditEventEntity, ?>> parameterExtractors
    ) {
        if (events.isEmpty()) {
            return;
        }
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (var statement = connection.prepareStatement(sql)) {
                for (AuditEventEntity event : events) {
                    for (int index = 0; index < parameterExtractors.size(); index++) {
                        statement.setObject(index + 1, parameterExtractors.get(index).apply(event));
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }
}
