/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.organization.jpa;

import java.util.Optional;
import java.util.UUID;

import org.activiti.cloud.organization.repository.ApplicationRepository;
import org.activiti.cloud.services.organization.entity.ApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link ApplicationEntity} entity
 */
@RepositoryRestResource(path = "applications",
        collectionResourceRel = "applications",
        itemResourceRel = "applications",
        exported = false)
public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, String>,
                                                  ApplicationRepository<ApplicationEntity> {

    @Override
    default Page<ApplicationEntity> getApplications(Pageable pageable) {
        return findAll(pageable);
    }

    @Override
    default Optional<ApplicationEntity> findApplicationById(String applicationId) {
        return findById(applicationId);
    }

    @Override
    default ApplicationEntity createApplication(ApplicationEntity application) {
        if (application.getId() == null) {
            application.setId(UUID.randomUUID().toString());
        }
        return save(application);
    }

    @Override
    default ApplicationEntity updateApplication(ApplicationEntity applicationToUpdate) {
        return save(applicationToUpdate);
    }

    @Override
    default void deleteApplication(ApplicationEntity application) {
        delete(application);
    }

    @Override
    default Class<ApplicationEntity> getApplicationType() {
        return ApplicationEntity.class;
    }
}
