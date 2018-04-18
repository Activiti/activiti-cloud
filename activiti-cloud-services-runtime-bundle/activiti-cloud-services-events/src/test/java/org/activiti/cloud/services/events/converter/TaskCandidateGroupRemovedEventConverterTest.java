package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.Application;
import org.activiti.cloud.services.api.model.Service;
import org.activiti.cloud.services.api.model.converter.TaskCandidateGroupConverter;
import org.activiti.cloud.services.events.TaskCandidateGroupRemovedEvent;
import org.activiti.cloud.services.events.builders.ApplicationBuilderService;
import org.activiti.cloud.services.events.builders.ServiceBuilderService;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.cloud.services.events.converter.EventConverterContext.getPrefix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskCandidateGroupRemovedEventConverterTest {

    @InjectMocks
    private TaskCandidateGroupRemovedEventConverter taskCandidateGroupRemovedEventConverter;

    @Mock
    private TaskCandidateGroupConverter taskCandidateGroupConverter;

    @Mock
    private ServiceBuilderService serviceBuilderService;

    @Mock
    private ApplicationBuilderService applicationBuilderService;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalEventToExternalEvent() throws Exception {
        //given
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_DELETED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("1");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");

        IdentityLink internalIdentityLink = mock(IdentityLink.class);
        given(activitiEvent.getEntity()).willReturn(internalIdentityLink);

        org.activiti.cloud.services.api.model.TaskCandidateGroup externalTaskCandidateGroup = mock(org.activiti.cloud.services.api.model.TaskCandidateGroup.class);
        given(taskCandidateGroupConverter.from(internalIdentityLink)).willReturn(externalTaskCandidateGroup);

        given(serviceBuilderService.buildService()).willReturn(new Service("myApp","myApp","runtime-bundle","1"));
        given(applicationBuilderService.buildApplication()).willReturn(new Application());


        //when
        ProcessEngineEvent pee = taskCandidateGroupRemovedEventConverter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(TaskCandidateGroupRemovedEvent.class);
        assertThat(pee.getExecutionId()).isEqualTo("1");
        assertThat(pee.getProcessInstanceId()).isEqualTo("1");
        assertThat(pee.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(pee.getService().getFullName()).isEqualTo("myApp");
        assertThat(((TaskCandidateGroupRemovedEvent) pee).getTaskCandidateGroup()).isEqualTo(externalTaskCandidateGroup);
    }

    @Test
    public void handledTypeShouldReturnTaskCandidateGroup() throws Exception {
        //when
        String activitiEventType = taskCandidateGroupRemovedEventConverter.handledType();
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_DELETED);
        IdentityLink internalIdentityLink = mock(IdentityLink.class);
        given(activitiEvent.getEntity()).willReturn(internalIdentityLink);
        given(internalIdentityLink.getGroupId()).willReturn("hr");
        given(internalIdentityLink.getType()).willReturn(IdentityLinkType.CANDIDATE);
        //then
        assertThat(activitiEventType).isEqualTo(getPrefix(activitiEvent) + ActivitiEventType.ENTITY_DELETED);
    }
}
