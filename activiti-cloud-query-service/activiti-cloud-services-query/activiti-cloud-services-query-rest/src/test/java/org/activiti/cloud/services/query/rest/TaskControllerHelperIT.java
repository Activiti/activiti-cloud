package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
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

    @Test
    public void shouldtest() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("1");
        taskRepository.save(taskEntity);

        TaskVariableEntity variableEntity = new TaskVariableEntity();
        variableEntity.setName("name");
        variableEntity.setValue("id");
        variableEntity.setTaskId("1");
        variableEntity.setTask(taskEntity);
        taskVariableRepository.save(variableEntity);

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
}
