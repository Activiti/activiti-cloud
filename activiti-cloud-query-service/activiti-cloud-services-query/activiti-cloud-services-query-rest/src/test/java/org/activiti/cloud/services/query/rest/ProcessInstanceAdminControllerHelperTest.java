package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceAdminControllerHelper;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
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
public class ProcessInstanceAdminControllerHelperTest {

    @InjectMocks
    private ProcessInstanceAdminControllerHelper processInstanceAdminControllerHelper;

    @Mock
    private ProcessInstanceRepository processInstanceRepository;

    @Mock
    private ProcessInstanceAdminService processInstanceAdminService;

    @Mock
    private ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Test
    public void findAllProcessInstanceAdmin_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceAdminService.findAll(predicate, pageable)).willReturn(pageResult);
        given(processInstanceControllerHelper.mapAllSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdmin(predicate, pageable);

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    public void findAllProcessInstanceAdminWithVariables_shouldReturnProcessInstances() {
        //given
        Predicate predicate = mock(Predicate.class);
        List<String> variableKeys = Collections.singletonList("var1");
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProcessInstanceEntity> pageResult = new PageImpl<>(Collections.singletonList(new ProcessInstanceEntity()));
        given(processInstanceAdminService.findAllWithVariables(predicate, variableKeys, pageable)).willReturn(pageResult);
        given(processInstanceControllerHelper.mapAllSubprocesses(pageResult, pageable)).willReturn(pageResult);

        //when
        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdminWithVariables(predicate, variableKeys, pageable);

        //then
        assertThat(result).isEqualTo(pageResult);
    }

    @Test
    public void findByIdProcessAdmin_shouldReturnProcessInstance() {
        //given
        String processInstanceId = "1";
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        given(processInstanceAdminService.findById(processInstanceId)).willReturn(processInstanceEntity);
        given(processInstanceRepository.mapSubprocesses(processInstanceEntity)).willReturn(processInstanceEntity);

        //when
        ProcessInstanceEntity result = processInstanceAdminControllerHelper.findByIdProcessAdmin(processInstanceId);

        //then
        assertThat(result).isEqualTo(processInstanceEntity);
    }
}
