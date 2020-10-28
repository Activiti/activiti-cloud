package org.activiti.cloud.starter.audit.tests.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.Map;
import org.activiti.api.model.shared.event.VariableEvent.VariableEvents;
import org.activiti.api.runtime.model.impl.VariableInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableCreatedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableDeletedEvent;
import org.activiti.cloud.api.model.shared.events.CloudVariableUpdatedEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableCreatedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableDeletedEventImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudVariableUpdatedEventImpl;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.activiti.cloud.starters.test.MyProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(EventsRestTemplate.class)
@ContextConfiguration(initializers ={ RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
class AuditVariableEventsIT {

    @Autowired
    private EventsRestTemplate eventsRestTemplate;

    @Autowired
    private EventsRepository repository;

    @Autowired
    private MyProducer producer;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    void should_supportAllCloudVariableEvents() {
        //given
        VariableInstanceImpl<String> variableInstance = new VariableInstanceImpl<>("variableName", "string", "initValue", "procInstId", null);
        CloudVariableCreatedEventImpl variableCreatedEvent = new CloudVariableCreatedEventImpl(variableInstance);
        VariableInstanceImpl<String> variableInstanceUpdate = new VariableInstanceImpl<>("variableName", "string", "updatedValue", "procInstId", null);
        CloudVariableUpdatedEventImpl<String> variableUpdatedEvent = new CloudVariableUpdatedEventImpl<>(variableInstanceUpdate, "initValue");
        CloudVariableDeletedEventImpl variableDeletedEvent = new CloudVariableDeletedEventImpl(variableInstanceUpdate);

        producer.send(variableCreatedEvent, variableUpdatedEvent, variableDeletedEvent);

        await().untilAsserted(() -> {
            //when
            ResponseEntity<PagedModel<CloudRuntimeEvent>> eventsPagedModel = eventsRestTemplate
                .executeFindAll();

            //then
            Collection<CloudRuntimeEvent> retrievedEvents = eventsPagedModel.getBody().getContent();
            assertThat(retrievedEvents)
                .hasSize(3)
                .hasOnlyElementsOfTypes(CloudVariableCreatedEventImpl.class, CloudVariableUpdatedEventImpl.class, CloudVariableDeletedEventImpl.class);

            Map<String, Object> createdFilter = Map.of("eventType", VariableEvents.VARIABLE_CREATED.name());
            ResponseEntity<PagedModel<CloudRuntimeEvent>> createdEventsPage = eventsRestTemplate.executeFind(createdFilter);
            Collection<CloudRuntimeEvent> createdEvents = createdEventsPage.getBody().getContent();
            assertThat(createdEvents)
                .hasSize(1)
                .hasOnlyElementsOfType(CloudVariableCreatedEventImpl.class);

            CloudVariableCreatedEvent createdEvent = (CloudVariableCreatedEventImpl) createdEvents.iterator().next();
            assertThat(createdEvent)
                .extracting(event -> event.getEntity().getName(),
                    event -> event.getEntity().getProcessInstanceId(),
                    event -> event.getEntity().getType(),
                    event -> event.getEntity().getValue())
                .containsExactly("variableName", "procInstId", "string", "initValue");

            Map<String, Object> updatedFilter = Map.of("eventType", VariableEvents.VARIABLE_UPDATED.name());
            ResponseEntity<PagedModel<CloudRuntimeEvent>> updatedEventsPage = eventsRestTemplate.executeFind(updatedFilter);

            Collection<CloudRuntimeEvent> updatedEvents = updatedEventsPage.getBody().getContent();
            assertThat(updatedEvents)
                .hasSize(1)
                .hasOnlyElementsOfType(CloudVariableUpdatedEventImpl.class);

            CloudVariableUpdatedEvent updatedEvent = (CloudVariableUpdatedEventImpl) updatedEvents.iterator().next();
            assertThat(updatedEvent)
                .extracting(event -> event.getEntity().getName(),
                    event -> event.getEntity().getProcessInstanceId(),
                    event -> event.getEntity().getType(),
                    event -> event.getEntity().getValue(),
                    event -> event.getPreviousValue())
                .containsExactly("variableName", "procInstId", "string", "updatedValue", "initValue");

            Map<String, Object> deletedFilter = Map.of("eventType", VariableEvents.VARIABLE_DELETED.name());
            ResponseEntity<PagedModel<CloudRuntimeEvent>> deletedEventsPage = eventsRestTemplate.executeFind(deletedFilter);
            Collection<CloudRuntimeEvent> deletedEvents = deletedEventsPage.getBody().getContent();
            assertThat(deletedEvents)
                .hasSize(1)
                .hasOnlyElementsOfType(CloudVariableDeletedEventImpl.class);

            CloudVariableDeletedEvent deletedEvent = (CloudVariableDeletedEventImpl) deletedEvents.iterator().next();
            assertThat(deletedEvent)
                .extracting(event -> event.getEntity().getName(),
                    event -> event.getEntity().getProcessInstanceId(),
                    event -> event.getEntity().getType(),
                    event -> event.getEntity().getValue())
                .containsExactly("variableName", "procInstId", "string", "updatedValue");
        });
    }

}
