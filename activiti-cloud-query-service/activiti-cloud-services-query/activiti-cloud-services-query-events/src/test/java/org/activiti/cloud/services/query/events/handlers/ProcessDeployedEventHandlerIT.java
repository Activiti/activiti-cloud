package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessDeployedEventImpl;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessModelEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest(
    classes = { QueryEventsTestApplication.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@Testcontainers
@Transactional
class ProcessDeployedEventHandlerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProcessDeployedEventHandler handler;

    @Test
    void shouldPersistProcessDefinitionAndModelInDatabase() {
        // given
        String processId = "test-process-id";
        ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl();
        processDefinition.setId(processId);
        processDefinition.setName("Test Process");
        processDefinition.setKey("TEST-PROCESS");
        processDefinition.setVersion(1);

        CloudProcessDeployedEventImpl event = new CloudProcessDeployedEventImpl(processDefinition);
        event.setAppName("test-app");
        event.setAppVersion("1.0");
        event.setServiceName("test-service");
        event.setServiceFullName("test-service-full");
        event.setServiceVersion("1.0");
        event.setServiceType("default");
        event.setProcessModelContent("<?xml version=\"1.0\" ?><bpmn2:definitions />");

        // when
        handler.handle(event);
        entityManager.flush();
        entityManager.clear();

        // then
        ProcessDefinitionEntity storedProcessDef = entityManager.find(ProcessDefinitionEntity.class, processId);
        assertThat(storedProcessDef).isNotNull();
        assertThat(storedProcessDef.getId()).isEqualTo(processId);
        assertThat(storedProcessDef.getName()).isEqualTo("Test Process");
        assertThat(storedProcessDef.getKey()).isEqualTo("TEST-PROCESS");
        assertThat(storedProcessDef.getVersion()).isEqualTo(1);
        assertThat(storedProcessDef.getServiceName()).isEqualTo("test-service");
        assertThat(storedProcessDef.getAppName()).isEqualTo("test-app");

        ProcessModelEntity storedProcessModel = entityManager.find(ProcessModelEntity.class, processId);
        assertThat(storedProcessModel).isNotNull();
        assertThat(storedProcessModel.getProcessDefinition().getId()).isEqualTo(processId);
        assertThat(storedProcessModel.getProcessModelContent()).isEqualTo("<?xml version=\"1.0\" ?><bpmn2:definitions />");
    }
}
