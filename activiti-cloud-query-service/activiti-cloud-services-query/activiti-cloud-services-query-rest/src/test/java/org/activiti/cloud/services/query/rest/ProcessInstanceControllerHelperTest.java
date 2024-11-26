package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class ProcessInstanceControllerHelperTest {

    @InjectMocks
    private ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessInstanceService processInstanceService;

    @Test
    public void findAllProcessInstances_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.findAll(predicate, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstances(
            predicate,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    public void findAllProcessInstancesWithVariables_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        List<String> variableKeys = Collections.singletonList("var1");
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.findAllWithVariables(predicate, variableKeys, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstancesWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    public void findById_shouldReturnProcessInstance() {
        //given
        String processInstanceId = "1";
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        given(processInstanceService.findById(processInstanceId)).willReturn(processInstanceEntity);
        given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

        //when
        ProcessInstanceEntity result = processInstanceControllerHelper.findById(processInstanceId);

        //then
        assertThat(result).isEqualTo(processInstanceEntity);
    }

    @Test
    public void searchProcessInstances_shouldReturnProcessInstances() {
        //given
        ProcessInstanceSearchRequest searchRequest = new ProcessInstanceSearchRequest(
            Set.of("My-app"), // processDefinitionKeys
            Set.of("initiator"), // initiators
            Set.of("1.0"), // appVersions
            Set.of(ProcessInstance.ProcessInstanceStatus.RUNNING), // statuses
            null, // lastModifiedFrom
            null, // lastModifiedTo
            null, // startFrom
            null, // startTo
            null, // completedFrom
            null, // completedTo
            null, // suspendedFrom
            null, // suspendedTo
            null, // processVariableFilters
            null, // processVariableKeys
            null // sort
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.search(searchRequest, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.searchProcessInstances(
            searchRequest,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    public void searchSubprocesses_shouldReturnSubprocesses() {
        //given
        String processInstanceId = "1";
        Predicate predicate = mock(Predicate.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceService.subprocesses(processInstanceId, predicate, pageable)).willReturn(pageResult);
        given(processInstanceRepository.mapSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.searchSubprocesses(
            processInstanceId,
            predicate,
            pageable
        );

        //then
        assertThat(result).isEqualTo(pageResult);
    }
}
