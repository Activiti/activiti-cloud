package org.activiti.cloud.services.rest.assemblers;

import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.impl.CloudTaskImpl;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;

import static org.activiti.api.task.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.api.task.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskResourceAssemblerTest {

    @InjectMocks
    private TaskResourceAssembler resourceAssembler;

    @Mock
    private ToCloudTaskConverter converter;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        TaskResource resource = resourceAssembler.toResource(model);

        Link selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isNotNull();
        assertThat(selfResourceLink.getHref()).contains("my-identifier");
    }

    @Test
    public void toResourceShouldReturnResourceWithReleaseAndCompleteLinksWhenStatusIsAssigned() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNotNull();
        assertThat(resource.getLink("release")).isNull();
        assertThat(resource.getLink("complete")).isNull();
    }

    @Test
    public void toResourceShouldReturnResourceWithClaimLinkWhenStatusIsNotAssigned() {
        Task model = new TaskImpl("my-identifier", "myTask", ASSIGNED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("claim")).isNull();
        assertThat(resource.getLink("release")).isNotNull();
        assertThat(resource.getLink("complete")).isNotNull();
    }

    @Test
    public void toResourceShouldNotReturnResourceWithProcessInstanceLinkWhenNewTaskIsCreated() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));
        TaskResource resource = resourceAssembler.toResource(model);

        // a new standalone task doesn't have a bond to a process instance
        // and should not return the rel 'processInstance'
        assertThat(resource.getLink("processInstance")).isNull();
    }

    @Test
    public void toResourceShouldReturnResourceWithProcessInstanceLinkForProcessInstanceTask() {
        // process instance task
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);
        ((TaskImpl) model).setProcessInstanceId("processInstanceId");

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        TaskResource resource = resourceAssembler.toResource(model);

        assertThat(resource.getLink("processInstance")).isNotNull();
    }

}