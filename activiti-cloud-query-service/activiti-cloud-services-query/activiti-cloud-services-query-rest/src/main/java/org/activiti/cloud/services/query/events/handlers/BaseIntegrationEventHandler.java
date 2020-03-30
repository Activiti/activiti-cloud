package org.activiti.cloud.services.query.events.handlers;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationEvent;
import org.activiti.cloud.services.query.app.repository.BPMNActivityRepository;
import org.activiti.cloud.services.query.app.repository.IntegrationContextRepository;
import org.activiti.cloud.services.query.model.BPMNActivityEntity;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.QueryException;

public abstract class BaseIntegrationEventHandler {

    protected final IntegrationContextRepository integrationContextRepository;
    protected final BPMNActivityRepository bpmnActivityRepository;

    public BaseIntegrationEventHandler(IntegrationContextRepository integrationContextRepository,
                                       BPMNActivityRepository bpmnActivityRepository) {
        this.integrationContextRepository = integrationContextRepository;
        this.bpmnActivityRepository = bpmnActivityRepository;
    }

    protected IntegrationContextEntity findOrCreateIntegrationContextEntity(CloudIntegrationEvent event) {

        IntegrationContext integrationContext = event.getEntity();

        IntegrationContextEntity entity = integrationContextRepository.findByProcessInstanceIdAndClientIdAndExecutionId(integrationContext.getProcessInstanceId(),
                                                                                                                        integrationContext.getClientId(),
                                                                                                                        integrationContext.getExecutionId());
        // Let's create entity if does not exists
        if(entity == null) {
            BPMNActivityEntity bpmnActivityEntity = bpmnActivityRepository.findByProcessInstanceIdAndElementIdAndExecutionId(integrationContext.getProcessInstanceId(),
                                                                                                                             integrationContext.getClientId(),
                                                                                                                             integrationContext.getExecutionId());
            entity = new IntegrationContextEntity(event.getServiceName(),
                                                  event.getServiceFullName(),
                                                  event.getServiceVersion(),
                                                  event.getAppName(),
                                                  event.getAppVersion());
            // Let use event id to persist integration context
            entity.setId(bpmnActivityEntity.getId());
            entity.setClientId(integrationContext.getClientId());
            entity.setClientName(integrationContext.getClientName());
            entity.setClientType(integrationContext.getClientType());
            entity.setConnectorType(integrationContext.getConnectorType());
            entity.setProcessDefinitionId(integrationContext.getProcessDefinitionId());
            entity.setProcessInstanceId(integrationContext.getProcessInstanceId());
            entity.setExecutionId(integrationContext.getExecutionId());
            entity.setProcessDefinitionKey(integrationContext.getProcessDefinitionKey());
            entity.setProcessDefinitionVersion(integrationContext.getProcessDefinitionVersion());
            entity.setBusinessKey(integrationContext.getBusinessKey());
            entity.setBpmnActivity(bpmnActivityEntity);
        }

        return entity;
    }

    protected void persistIntoDatabase(CloudRuntimeEvent<?, ?> event,
                                       IntegrationContextEntity entity) {
        try {
            integrationContextRepository.save(entity);
        } catch (Exception cause) {
            throw new QueryException("Error handling CloudIntegrationEvent[" + event + "]",
                                     cause);
        }
    }
}
