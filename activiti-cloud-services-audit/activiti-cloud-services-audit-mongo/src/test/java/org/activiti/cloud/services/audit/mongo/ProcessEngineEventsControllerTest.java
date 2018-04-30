package org.activiti.cloud.services.audit.mongo;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.audit.mongo.assembler.EventResourceAssembler;
import org.activiti.cloud.services.audit.mongo.events.ProcessEngineEventDocument;
import org.activiti.cloud.services.audit.mongo.repository.EventsRepository;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessEngineEventsControllerTest {

    @InjectMocks
    private ProcessEngineEventsController processEngineEventsController;

    @Mock
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private EventResourceAssembler eventResourceAssembler;

    @Mock
    private AlfrescoPagedResourcesAssembler<ProcessEngineEventDocument> pagedResourcesAssembler;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void findByIdShouldCallFindById() throws Exception {
        ProcessEngineEventDocument event = mock(ProcessEngineEventDocument.class);
        when(eventsRepository.findById(any())).thenReturn(Optional.of(event));
        when(securityPoliciesApplicationService.canRead(any(),any())).thenReturn(true);

        processEngineEventsController.findById("1");

        verify(eventsRepository).findById("1");

    }

    @Test
    public void findByIdShouldErrorWhenNotPermitted() throws Exception {
        ProcessEngineEventDocument event = mock(ProcessEngineEventDocument.class);
        when(eventsRepository.findById(any())).thenReturn(Optional.of(event));
        when(securityPoliciesApplicationService.canRead(any(),any())).thenReturn(false);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->processEngineEventsController.findById("1"));

    }

    @Test
    public void findAllShouldCallFindAll() throws Exception {
        ProcessEngineEventDocument event = mock(ProcessEngineEventDocument.class);
        when(securityPoliciesApplicationService.restrictProcessEngineEventQuery(any(),any())).thenReturn(null);

        processEngineEventsController.findAll(null,null);

        verify(eventsRepository).findAll((Predicate) null,(Pageable) null);

    }
}
