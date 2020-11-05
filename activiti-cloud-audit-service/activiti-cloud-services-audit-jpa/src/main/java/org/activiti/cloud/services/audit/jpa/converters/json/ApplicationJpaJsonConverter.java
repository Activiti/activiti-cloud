package org.activiti.cloud.services.audit.jpa.converters.json;

import org.activiti.api.process.model.Deployment;

public class ApplicationJpaJsonConverter extends JpaJsonConverter<Deployment> {

    public ApplicationJpaJsonConverter() {
        super(Deployment.class);
    }
}
