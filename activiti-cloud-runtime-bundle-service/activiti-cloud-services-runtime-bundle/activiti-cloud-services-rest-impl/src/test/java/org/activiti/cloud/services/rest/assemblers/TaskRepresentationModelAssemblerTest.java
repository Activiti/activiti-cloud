package org.activiti.cloud.services.rest.assemblers;

import java.util.Optional;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.impl.CloudTaskImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;

import static org.activiti.api.task.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.api.task.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskRepresentationModelAssemblerTest {

    @InjectMocks
    private TaskRepresentationModelAssembler representationModelAssembler;

    @Mock
    private ToCloudTaskConverter converter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void toResourceShouldReturnResourceWithSelfLinkContainingResourceId() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        EntityModel<CloudTask> resource = representationModelAssembler.toModel(model);

        Optional<Link> selfResourceLink = resource.getLink("self");

        assertThat(selfResourceLink).isPresent();
        assertThat(selfResourceLink.get().getHref()).contains("my-identifier");
    }

    @Test
    public void toResourceShouldReturnResourceWithReleaseAndCompleteLinksWhenStatusIsAssigned() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        EntityModel<CloudTask> resource = representationModelAssembler.toModel(model);

        assertThat(resource.getLink("claim")).isPresent();
        assertThat(resource.getLink("release")).isNotPresent();
        assertThat(resource.getLink("complete")).isNotPresent();
    }

    @Test
    public void toResourceShouldReturnResourceWithClaimLinkWhenStatusIsNotAssigned() {
        Task model = new TaskImpl("my-identifier", "myTask", ASSIGNED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        EntityModel<CloudTask> resource = representationModelAssembler.toModel(model);

        assertThat(resource.getLink("claim")).isNotPresent();
        assertThat(resource.getLink("release")).isPresent();
        assertThat(resource.getLink("complete")).isPresent();
    }

    @Test
    public void toResourceShouldNotReturnResourceWithProcessInstanceLinkWhenNewTaskIsCreated() {
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));
        EntityModel<CloudTask> resource = representationModelAssembler.toModel(model);

        // a new standalone task doesn't have a bond to a process instance
        // and should not return the rel 'processInstance'
        assertThat(resource.getLink("processInstance")).isNotPresent();
    }

    @Test
    public void toResourceShouldReturnResourceWithProcessInstanceLinkForProcessInstanceTask() {
        // process instance task
        Task model = new TaskImpl("my-identifier", "myTask", CREATED);
        ((TaskImpl) model).setProcessInstanceId("processInstanceId");

        given(converter.from(model)).willReturn(new CloudTaskImpl(model));

        EntityModel<CloudTask> resource = representationModelAssembler.toModel(model);

        assertThat(resource.getLink("processInstance")).isPresent();
    }

}
