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

import org.activiti.cloud.organization.core.model.Application;
import org.activiti.cloud.organization.core.repository.ApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link Application} entity
 */
@RepositoryRestResource(path = "applications",
        collectionResourceRel = "applications",
        itemResourceRel = "applications")
public interface ApplicationJpaRepository extends JpaRepository<Application, String>,
                                                  ApplicationRepository {

    @Override
    default Page<Application> getApplications(Pageable pageable) {
        return findAll(pageable);
    }

    @Override
    default Optional<Application> findApplicationById(String applicationId) {
        return findById(applicationId);
    }

    @Override
    default Application createApplication(Application application) {
        return save(application);
    }

    @Override
    default Application updateApplication(Application applicationToUpdate) {
        return save(applicationToUpdate);
    }

    @Override
    default void deleteApplication(Application application) {
        delete(application);
    }
}
