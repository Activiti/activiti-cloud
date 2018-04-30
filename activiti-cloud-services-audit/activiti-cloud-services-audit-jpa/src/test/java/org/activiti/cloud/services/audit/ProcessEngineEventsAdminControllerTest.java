package org.activiti.cloud.services.audit;

import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.audit.assembler.EventResourceAssembler;
import org.activiti.cloud.services.audit.events.ProcessEngineEventEntity;
import org.activiti.cloud.services.audit.repository.EventsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessEngineEventsAdminControllerTest {

    @InjectMocks
    private ProcessEngineEventsAdminController processEngineEventsController;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private EventResourceAssembler eventResourceAssembler;

    @Mock
    private AlfrescoPagedResourcesAssembler<ProcessEngineEventEntity> pagedResourcesAssembler;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void findAllShouldCallFindAll() throws Exception {
        ProcessEngineEventEntity event = mock(ProcessEngineEventEntity.class);

        processEngineEventsController.findAll(null,null);

        verify(eventsRepository).findAll(null,(Pageable) null);

    }
}
