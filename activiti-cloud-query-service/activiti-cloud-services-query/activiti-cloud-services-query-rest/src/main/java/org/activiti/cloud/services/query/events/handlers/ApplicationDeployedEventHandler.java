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

import javax.persistence.EntityManager;
import org.activiti.api.process.model.Deployment;
import org.activiti.api.process.model.events.ApplicationEvent.ApplicationEvents;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.events.CloudApplicationDeployedEvent;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDeployedEventHandler implements QueryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDeployedEventHandler.class);

    private final EntityManager entityManager;
    private final ApplicationRepository applicationRepository;

    public ApplicationDeployedEventHandler(EntityManager entityManager, ApplicationRepository applicationRepository) {
        this.entityManager = entityManager;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public void handle(CloudRuntimeEvent<?, ?> event) {
        CloudApplicationDeployedEvent applicationDeployedEvent = (CloudApplicationDeployedEvent) event;
        Deployment deployment = applicationDeployedEvent.getEntity();
        LOGGER.debug("Handling application deployed event for " + deployment.getId());
        ApplicationEntity application = new ApplicationEntity(
            deployment.getId(),
            applicationDeployedEvent.getAppName(),
            deployment.getVersion().toString()
        );

        if (applicationRepository.existsByNameAndVersion(application.getName(), application.getVersion())) {
            LOGGER.debug(
                "Application {} with version {} already exists!",
                application.getName(),
                application.getVersion()
            );
            return;
        }

        entityManager.persist(application);
    }

    @Override
    public String getHandledEvent() {
        return ApplicationEvents.APPLICATION_DEPLOYED.name();
    }
}
