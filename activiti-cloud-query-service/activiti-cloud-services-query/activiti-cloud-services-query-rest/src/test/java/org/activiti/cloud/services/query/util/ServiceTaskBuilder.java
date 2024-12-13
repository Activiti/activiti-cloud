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
package org.activiti.cloud.services.query.util;

import java.util.Date;
import java.util.UUID;
import org.activiti.cloud.services.query.app.repository.ServiceTaskRepository;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;

public class ServiceTaskBuilder {

    private final ServiceTaskRepository repository;

    private final ServiceTaskEntity serviceTask;

    public ServiceTaskBuilder(ServiceTaskRepository repository) {
        this.repository = repository;
        this.serviceTask =
            new ServiceTaskEntity("serviceName", "serviceFullName", "serviceVersion", "appName", "appVersion");
        this.serviceTask.setId(UUID.randomUUID().toString());
        this.serviceTask.setActivityType("serviceTask");
    }

    public ServiceTaskBuilder withId(String id) {
        serviceTask.setId(id);
        return this;
    }

    public ServiceTaskBuilder withStartedDate(Date startedDate) {
        serviceTask.setStartedDate(startedDate);
        return this;
    }

    public ServiceTaskBuilder withCompletedDate(Date completedDate) {
        serviceTask.setCompletedDate(completedDate);
        return this;
    }

    public ServiceTaskEntity save() {
        return repository.save(serviceTask);
    }
}
