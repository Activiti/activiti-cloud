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

import java.util.Arrays;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Keeps the {@code process_instance_hierarchy} closure table in sync when an existing process instance
 * gets linked to a main process after it has been started (e.g. via the link process instances endpoint),
 * a change that no runtime event notifies.
 *
 * <p>The rows are written with a single {@code INSERT ... SELECT} on the session connection because
 * entities cannot safely be persisted from within a flush callback.
 */
public class ProcessInstanceLinkHierarchyListener implements PostUpdateEventListener {

    private static final String LINKED_PROCESS_INSTANCE_ID_PROPERTY = "linkedProcessInstanceId";

    private static final String REGISTER_LINKED_PROCESS_SQL =
        "INSERT INTO process_instance_hierarchy (ancestor_id, descendant_id, depth, relation_type) " +
        "SELECT a.ancestor_id, d.descendant_id, a.depth + 1 + d.depth, " +
        "CASE WHEN d.depth = 0 THEN '" +
        ProcessInstanceHierarchyEntity.RELATION_LINKED +
        "' ELSE d.relation_type END " +
        "FROM process_instance_hierarchy a, process_instance_hierarchy d " +
        "WHERE a.descendant_id = ? AND d.ancestor_id = ? " +
        "AND NOT EXISTS (SELECT 1 FROM process_instance_hierarchy x " +
        "WHERE x.ancestor_id = a.ancestor_id AND x.descendant_id = d.descendant_id)";

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!(event.getEntity() instanceof ProcessInstanceEntity processInstance) || event.getOldState() == null) {
            return;
        }

        EntityPersister persister = event.getPersister();
        int index = Arrays.asList(persister.getPropertyNames()).indexOf(LINKED_PROCESS_INSTANCE_ID_PROPERTY);

        if (index >= 0 && event.getOldState()[index] == null && event.getState()[index] != null) {
            String linkedProcessInstanceId = (String) event.getState()[index];
            event.getSession().doWork(connection -> {
                try (var statement = connection.prepareStatement(REGISTER_LINKED_PROCESS_SQL)) {
                    statement.setString(1, linkedProcessInstanceId);
                    statement.setString(2, processInstance.getId());
                    statement.executeUpdate();
                }
            });
        }
    }
}
