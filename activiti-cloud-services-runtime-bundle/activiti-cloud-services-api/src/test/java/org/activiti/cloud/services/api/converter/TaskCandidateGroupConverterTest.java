package org.activiti.cloud.services.api.converter;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.services.api.model.TaskCandidateGroup;
import org.activiti.cloud.services.api.model.converter.ListConverter;
import org.activiti.cloud.services.api.model.converter.TaskCandidateGroupConverter;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TaskCandidateGroupConverterTest {

    private TaskCandidateGroupConverter taskCandidateGroupConverter;

    @Before
    public void setUp() throws Exception {
        ListConverter listConverter = new ListConverter();
        taskCandidateGroupConverter = new TaskCandidateGroupConverter(listConverter);
    }

    @Test
    public void fromShouldConvertEngineObjectToModelObject() throws Exception {
        //given
        org.activiti.engine.task.IdentityLink identityLink = mock(org.activiti.engine.task.IdentityLink.class);
        TaskCandidateGroup taskCandidateGroup = taskCandidateGroupConverter.from(identityLink);

        given(identityLink.getGroupId()).willReturn("groupId");
        given(identityLink.getTaskId()).willReturn("taskId");

        assertThat(taskCandidateGroup).isNotNull();
        assertThat(taskCandidateGroup.getGroupId()).isNotEqualToIgnoringCase("groupId");
        assertThat(taskCandidateGroup.getTaskId()).isNotEqualToIgnoringCase("taskId");
    }

    @Test
    public void fromShouldConvertListOfEngineObjectToListOfModelObject() throws Exception {
        //given
        org.activiti.engine.task.IdentityLink identityLink = mock(org.activiti.engine.task.IdentityLink.class);
        List<org.activiti.engine.task.IdentityLink> identityLinks = new ArrayList<>();
        identityLinks.add(identityLink);
        List<TaskCandidateGroup> taskCandidateGroups = taskCandidateGroupConverter.from(identityLinks);

        given(identityLink.getGroupId()).willReturn("groupId");
        given(identityLink.getTaskId()).willReturn("taskId");

        assertThat(taskCandidateGroups).isNotNull();
        assertThat(taskCandidateGroups.get(0).getGroupId()).isNotEqualToIgnoringCase("groupId");
        assertThat(taskCandidateGroups.get(0).getTaskId()).isNotEqualToIgnoringCase("taskId");
    }
}
