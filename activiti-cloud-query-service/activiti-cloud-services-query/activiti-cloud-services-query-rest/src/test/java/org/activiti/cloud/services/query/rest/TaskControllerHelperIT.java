package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.main.banner-mode=off")
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public class TaskControllerHelperIT {

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private org.activiti.cloud.services.query.app.repository.VariableRepository variableRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @Transactional
    public void shouldtest() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("15");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
        processVariableEntity.setName("name");
        processVariableEntity.setValue("id");
        processVariableEntity.setProcessInstanceId("15");
        processVariableEntity.setProcessInstance(processInstanceRepository.findById("15").orElseThrow());
        variableRepository.save(processVariableEntity);

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("1");
        taskEntity.setProcessVariables(Set.of(processVariableEntity));
        taskEntity.setProcessInstance(processInstanceEntity);
        taskEntity.setProcessInstanceId("15");
        taskRepository.save(taskEntity);

        Set<ProcessVariableEntity> variables = processInstanceRepository.findById("15").get().getVariables();
        //Long id = variables.iterator().next().getId();
        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);
        Pageable pageable = Pageable.ofSize(10);
        List<QueryDslPredicateFilter> filters = Collections.emptyList();
        List<String> processVariableKeys = List.of("name");
        PagedModel<EntityModel<QueryCloudTask>> allWithProcessVariables = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableKeys
        );
        List<EntityModel<QueryCloudTask>> entityModels = allWithProcessVariables.getContent().stream().toList();
        assertThat(entityModels).isNotEmpty();
        assertThat(entityModels).hasSize(1);
        EntityModel<QueryCloudTask> queryCloudTaskEntityModel = entityModels.get(0);
        assertThat(queryCloudTaskEntityModel.getContent().getProcessVariables().stream().toList().get(0).getName())
            .isEqualTo("name");
    }

    private ProcessVariableEntity buildVariable() {
        ProcessVariableEntity variableEntity = new ProcessVariableEntity(
            1L,
            String.class.getName(),
            "firstName",
            UUID.randomUUID().toString(),
            "My app",
            "My app",
            "1",
            null,
            null,
            new Date(),
            new Date(),
            UUID.randomUUID().toString()
        );
        variableEntity.setValue("John");
        return variableEntity;
    }
}
