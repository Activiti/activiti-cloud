package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.querydsl.core.types.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.*;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
@Testcontainers
@WithMockUser
public class ProcessInstanceControllerHelperIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private SecurityPoliciesManager securityPoliciesManager;

    @BeforeEach
    public void setUp() {
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldReturnAllProcessInstances() {
        ProcessInstanceEntity processInstanceEntity = buildDefaultProcessInstance();
        processInstanceEntity.setInitiator("testuser");
        processInstanceRepository.save(processInstanceEntity);
        //given
        given(
            securityPoliciesManager.canRead(
                processInstanceEntity.getProcessDefinitionKey(),
                processInstanceEntity.getServiceName()
            )
        )
            .willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstances(
            predicate,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);

        ProcessInstanceEntity returnedProcessInstance = verifyReturnedProcessInstanceEntity(
            result,
            processInstanceEntity
        );

        assertThat(returnedProcessInstance).isNotNull();
        assertThat(returnedProcessInstance.getSubprocesses()).isEmpty();
    }

    @Test
    public void shouldReturnAllProcessInstancesWithVariables() {
        ProcessInstanceEntity processInstanceEntity = buildDefaultProcessInstance();
        processInstanceEntity.setInitiator("testuser");
        processInstanceRepository.save(processInstanceEntity);
        //given
        given(
            securityPoliciesManager.canRead(
                processInstanceEntity.getProcessDefinitionKey(),
                processInstanceEntity.getServiceName()
            )
        )
            .willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity, 8);
        List<String> variableKeys = variables.stream().map(ProcessVariableEntity::getName).collect(Collectors.toList());
        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstancesWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);
    }

    @Test
    public void shouldReturnProcessInstanceById() {
        ProcessInstanceEntity parentProcessInstance = buildDefaultProcessInstance();
        parentProcessInstance.setInitiator("testuser");
        processInstanceRepository.save(parentProcessInstance);
        ProcessInstanceEntity subprocessInstance = buildSubprocessInstance(parentProcessInstance);

        processInstanceRepository.save(parentProcessInstance);
        processInstanceRepository.save(subprocessInstance);
        //given
        given(
            securityPoliciesManager.canRead(
                parentProcessInstance.getProcessDefinitionKey(),
                parentProcessInstance.getServiceName()
            )
        )
            .willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        String processInstanceId = parentProcessInstance.getId();

        ProcessInstanceEntity result = processInstanceControllerHelper.findById(processInstanceId);

        assertThat(result).isEqualTo(parentProcessInstance);
    }

    private ProcessInstanceEntity buildDefaultProcessInstance() {
        return new ProcessInstanceEntity(
            "My-app",
            "My-app",
            "1",
            null,
            null,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            ProcessInstance.ProcessInstanceStatus.RUNNING,
            new Date()
        );
    }

    private ProcessInstanceEntity buildSubprocessInstance(ProcessInstanceEntity parentProcessInstance) {
        ProcessInstanceEntity subprocessInstance = new ProcessInstanceEntity();
        subprocessInstance.setId(UUID.randomUUID().toString());
        subprocessInstance.setProcessDefinitionKey("mySubprocess");
        subprocessInstance.setProcessDefinitionName("subprocess");
        subprocessInstance.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        subprocessInstance.setParentId(parentProcessInstance.getId());
        return subprocessInstance;
    }

    private Set<ProcessVariableEntity> createProcessVariables(ProcessInstanceEntity processInstanceEntity, int count) {
        Set<ProcessVariableEntity> variables = new HashSet<>();
        for (int i = 0; i < count; i++) {
            ProcessVariableEntity variable = new ProcessVariableEntity();
            variable.setName("name" + i);
            variable.setValue("id");
            variables.add(variable);
        }
        return variables;
    }

    private ProcessInstanceEntity verifyReturnedProcessInstanceEntity(
        Page<ProcessInstanceEntity> result,
        ProcessInstanceEntity parentProcessInstance
    ) {
        return result
            .getContent()
            .stream()
            .filter(pi -> pi.getId().equals(parentProcessInstance.getId()))
            .findFirst()
            .orElse(null);
    }
}
