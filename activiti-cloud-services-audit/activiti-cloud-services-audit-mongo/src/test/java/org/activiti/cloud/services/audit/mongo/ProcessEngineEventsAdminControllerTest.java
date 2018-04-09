package org.activiti.cloud.services.audit.mongo;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.audit.mongo.assembler.EventResourceAssembler;
import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.activiti.cloud.services.audit.mongo.repository.EventsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;

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
    private PagedResourcesAssembler<ProcessEngineEventDocument> pagedResourcesAssembler;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void findAllShouldCallFindAll() throws Exception {
        ProcessEngineEventDocument event = mock(ProcessEngineEventDocument.class);

        processEngineEventsController.findAll(null,null);

        verify(eventsRepository).findAll((Predicate) null,(Pageable) null);

    }
}
