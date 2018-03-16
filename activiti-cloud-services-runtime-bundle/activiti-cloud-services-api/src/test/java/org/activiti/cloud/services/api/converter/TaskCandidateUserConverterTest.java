package org.activiti.cloud.services.api.converter;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.services.api.model.TaskCandidateUser;
import org.activiti.cloud.services.api.model.converter.ListConverter;
import org.activiti.cloud.services.api.model.converter.TaskCandidateUserConverter;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class TaskCandidateUserConverterTest {

    private TaskCandidateUserConverter taskCandidateUserConverter;

    @Before
    public void setUp() throws Exception {
        ListConverter listConverter = new ListConverter();
        taskCandidateUserConverter = new TaskCandidateUserConverter(listConverter);
    }

    @Test
    public void fromShouldConvertEngineObjectToModelObject() throws Exception {
        //given
        org.activiti.engine.task.IdentityLink identityLink = mock(org.activiti.engine.task.IdentityLink.class);
        TaskCandidateUser taskCandidateUser = taskCandidateUserConverter.from(identityLink);

        given(identityLink.getUserId()).willReturn("userId");
        given(identityLink.getTaskId()).willReturn("taskId");

        assertThat(taskCandidateUser).isNotNull();
        assertThat(taskCandidateUser.getUserId()).isNotEqualToIgnoringCase("userId");
        assertThat(taskCandidateUser.getTaskId()).isNotEqualToIgnoringCase("taskId");
    }

    @Test
    public void fromShouldConvertListOfEngineObjectToListOfModelObject() throws Exception {
        //given
        org.activiti.engine.task.IdentityLink identityLink = mock(org.activiti.engine.task.IdentityLink.class);
        List<org.activiti.engine.task.IdentityLink> identityLinks = new ArrayList<>();
        identityLinks.add(identityLink);
        List<TaskCandidateUser> taskCandidateUsers = taskCandidateUserConverter.from(identityLinks);

        given(identityLink.getUserId()).willReturn("userId");
        given(identityLink.getTaskId()).willReturn("taskId");

        assertThat(taskCandidateUsers).isNotNull();
        assertThat(taskCandidateUsers.get(0).getUserId()).isNotEqualToIgnoringCase("userId");
        assertThat(taskCandidateUsers.get(0).getTaskId()).isNotEqualToIgnoringCase("taskId");
    }
}
