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
package org.activiti.cloud.services.query.events.handlers;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance.ProcessInstanceStatus;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessDeletedEvent;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.BPMNSequenceFlowEntity;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;

public class ProcessDeletedEventHandler implements QueryEventHandler {

    protected final String INVALID_PROCESS_INSTANCE_STATE =
        "Process Instance %s is not in a valid state: %s. " +
        "Only process instances in status COMPLETED or CANCELLED can be deleted.";

    private Set<ProcessInstanceStatus> ALLOWED_STATUS = Set.of(
        ProcessInstanceStatus.CANCELLED,
        ProcessInstanceStatus.COMPLETED
    );

    private final EntityManager entityManager;

    public ProcessDeletedEventHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudProcessDeletedEvent deletedEvent = (CloudProcessDeletedEvent) event;

        var eventProcessInstanceId = deletedEvent.getEntity().getId();

        ProcessInstanceEntity processInstanceEntity = Optional
            .ofNullable(entityManager.find(ProcessInstanceEntity.class, eventProcessInstanceId))
            .orElseThrow(() ->
                new QueryException("Unable to find process instance with the given id: " + eventProcessInstanceId)
            );

        if (ALLOWED_STATUS.contains(processInstanceEntity.getStatus())) {
            remove(
                TaskCandidateUserEntity.class,
                "taskId",
                TaskEntity.class,
                "id",
                "processInstanceId",
                eventProcessInstanceId
            );

            remove(
                TaskCandidateGroupEntity.class,
                "taskId",
                TaskEntity.class,
                "id",
                "processInstanceId",
                eventProcessInstanceId
            );

            remove(TaskVariableEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(TaskEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(ProcessVariableEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(IntegrationContextEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(ServiceTaskEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(BPMNActivityEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(BPMNSequenceFlowEntity.class, "processInstanceId", eventProcessInstanceId);
            remove(ProcessInstanceEntity.class, "id", eventProcessInstanceId);
        } else {
            throw new IllegalStateException(
                String.format(
                    INVALID_PROCESS_INSTANCE_STATE,
                    processInstanceEntity.getId(),
                    processInstanceEntity.getStatus().name()
                )
            );
        }
    }

    <T> void remove(Class<T> entityClass, String attributeName, Object attributeValue) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();

        var delete = criteriaBuilder.createCriteriaDelete(entityClass);
        var from = delete.from(entityClass);

        delete.where(criteriaBuilder.equal(from.get(attributeName), attributeValue));

        entityManager.createQuery(delete).executeUpdate();
    }

    <P, T> void remove(
        Class<T> entityClass,
        String attributeName,
        Class<P> parentClass,
        String parentIdAttribute,
        String parentAttributeName,
        Object parentAttributeValue
    ) {
        var criteriaBuilder = entityManager.getCriteriaBuilder();

        var parentQuery = criteriaBuilder.createQuery(Object.class);
        var parentFrom = parentQuery.from(parentClass);
        parentQuery.select(parentFrom.get(parentIdAttribute));
        parentQuery.where(criteriaBuilder.equal(parentFrom.get(parentAttributeName), parentAttributeValue));

        var parentIds = entityManager.createQuery(parentQuery).getResultList();

        if (!parentIds.isEmpty()) {
            var delete = criteriaBuilder.createCriteriaDelete(entityClass);
            var from = delete.from(entityClass);

            delete.where(from.get(attributeName).in(parentIds));

            entityManager.createQuery(delete).executeUpdate();
        }
    }

    @Override
    public String getHandledEvent() {
        return ProcessRuntimeEvent.ProcessEvents.PROCESS_DELETED.name();
    }
}
