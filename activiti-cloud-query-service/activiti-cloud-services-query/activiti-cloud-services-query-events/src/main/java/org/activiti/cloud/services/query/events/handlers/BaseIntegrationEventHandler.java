/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;

public abstract class BaseIntegrationEventHandler {

    protected final EntityManager entityManager;

    public BaseIntegrationEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected Optional<IntegrationContextEntity> findIntegrationContextEntity(CloudIntegrationEvent event) {
        IntegrationContext integrationContext = event.getEntity();
        String pkId = integrationContext.getId();

        IntegrationContextEntity entity = entityManager.find(IntegrationContextEntity.class, pkId);

        // Fallback to previous primary key strategy for backward compatibility
        if (entity == null) {
            pkId = IntegrationContextEntity.IdBuilderHelper.from(integrationContext);
            entity = entityManager.find(IntegrationContextEntity.class, pkId);
        }

        return Optional.ofNullable(entity);
    }
}
