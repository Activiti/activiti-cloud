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
