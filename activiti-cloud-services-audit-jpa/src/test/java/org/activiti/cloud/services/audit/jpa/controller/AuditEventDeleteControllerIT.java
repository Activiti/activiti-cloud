package org.activiti.cloud.services.audit.jpa.controller;

import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.audit.api.resources.EventsRelProvider;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsDeleteController;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.resourcesResponseFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties="activiti.rest.enable-deletion=true")
@RunWith(SpringRunner.class)
@WebMvcTest(AuditEventsDeleteController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class AuditEventDeleteControllerIT {

    private static final String DOCUMENTATION_ALFRESCO_IDENTIFIER = "events-alfresco";

    @MockBean
    private EventsRepository eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;

    @Before
    public void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("admin");
        assertThat(userGroupManager).isNotNull();
    }

    @Test
    public void deleteEventsShouldReturnAllEventsAndDeleteThem() throws Exception {

        //given
        List<AuditEventEntity> list = buildEventsData(1);
        given(eventsRepository.findAll())
                .willReturn(list);

        //when
        mockMvc.perform(delete("/admin/v1/" + EventsRelProvider.COLLECTION_RESOURCE_REL)
                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_ALFRESCO_IDENTIFIER + "/list",
                        resourcesResponseFields()
                ));

        verify(eventsRepository).deleteAll(list);
    }

    private List<AuditEventEntity> buildEventsData(int recordsNumber) {

        List<AuditEventEntity> eventsList = new ArrayList<>();

        for (long i = 0; i < recordsNumber; i++) {
            //would like to mock this but jackson and mockito not happy together
            AuditEventEntity eventEntity = buildAuditEventEntity(i);
            eventsList.add(eventEntity);
        }

        return eventsList;
    }

    private AuditEventEntity buildAuditEventEntity(long id) {
        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity("id",
                System.currentTimeMillis());
        eventEntity.setId(id);
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("1");
        eventEntity.setProcessInstance(processInstance);
        eventEntity.setServiceName("rb-my-app");
        eventEntity.setEventType(ProcessRuntimeEvent.ProcessEvents.PROCESS_STARTED.name());
        eventEntity.setProcessDefinitionId("1");
        eventEntity.setProcessInstanceId("10");
        eventEntity.setTimestamp(System.currentTimeMillis());
        return eventEntity;
    }



}
