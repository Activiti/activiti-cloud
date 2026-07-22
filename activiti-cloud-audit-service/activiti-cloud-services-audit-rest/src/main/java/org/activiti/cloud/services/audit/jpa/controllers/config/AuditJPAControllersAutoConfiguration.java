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
package org.activiti.cloud.services.audit.jpa.controllers.config;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventType;
import org.activiti.cloud.services.audit.jpa.assembler.EventRepresentationModelAssembler;
import org.activiti.cloud.services.audit.jpa.assembler.config.EventRepresentationModelAssemblerConfiguration;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsAdminControllerImpl;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsControllerImpl;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsDeleteController;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsExporter;
import org.activiti.cloud.services.audit.jpa.controllers.v2.AuditEventsAdminControllerV2Impl;
import org.activiti.cloud.services.audit.jpa.controllers.v2.AuditEventsControllerV2Impl;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.audit.jpa.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsAdminService;
import org.activiti.cloud.services.audit.jpa.service.AuditEventsService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@Import(
    {
        EventRepresentationModelAssemblerConfiguration.class,
        AuditEventsAdminControllerImpl.class,
        AuditEventsControllerImpl.class,
        AuditEventsControllerV2Impl.class,
        AuditEventsAdminControllerV2Impl.class,
        AuditEventsDeleteController.class,
    }
)
public class AuditJPAControllersAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RepositoryRestConfigurer dataRestRepositoryRestConfigurer() {
        return new RepositoryRestConfigurer() {
            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
                config.disableDefaultExposure();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventsExporter auditEventsExporter(ObjectMapper objectMapper) {
        return new AuditEventsExporter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventsAdminService auditEventsAdminService(
        EventsRepository<AuditEventEntity> eventsRepository,
        APIEventToEntityConverters eventConverters,
        AuditEventsExporter auditEventsExporter,
        EventRepresentationModelAssembler eventRepresentationModelAssembler,
        AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler
    ) {
        return new AuditEventsAdminService(
            eventsRepository,
            eventConverters,
            auditEventsExporter,
            eventRepresentationModelAssembler,
            pagedCollectionModelAssembler
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventsService auditEventsService(
        EventsRepository<AuditEventEntity> eventsRepository,
        EventRepresentationModelAssembler eventRepresentationModelAssembler,
        APIEventToEntityConverters eventConverters,
        SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService,
        AlfrescoPagedModelAssembler<CloudRuntimeEvent<?, CloudRuntimeEventType>> pagedCollectionModelAssembler
    ) {
        return new AuditEventsService(
            eventsRepository,
            eventRepresentationModelAssembler,
            eventConverters,
            securityPoliciesApplicationService,
            pagedCollectionModelAssembler
        );
    }
}
