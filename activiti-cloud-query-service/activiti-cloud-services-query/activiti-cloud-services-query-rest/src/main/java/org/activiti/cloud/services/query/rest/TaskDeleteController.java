package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.assembler.TaskResourceAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;

@ConditionalOnProperty(name = "activiti.rest.enable-deletion", matchIfMissing = true)
@RestController
@RequestMapping(
        value = "/admin/v1/tasks",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class TaskDeleteController {

    private final TaskRepository taskRepository;

    private TaskResourceAssembler taskResourceAssembler;

    @Autowired
    public TaskDeleteController(TaskRepository taskRepository,
                                TaskResourceAssembler taskResourceAssembler) {
        this.taskRepository = taskRepository;
        this.taskResourceAssembler = taskResourceAssembler;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Resources<Resource<CloudTask>> deleteTasks (@QuerydslPredicate(root = TaskEntity.class) Predicate predicate) {

        Collection <Resource<CloudTask>> result = new ArrayList<>();
        Iterable <TaskEntity> iterable = taskRepository.findAll(predicate);

        for(TaskEntity entity : iterable){
            result.add(taskResourceAssembler.toResource(entity));
        }

        taskRepository.deleteAll(iterable);

        return new Resources<>(result);
    }

}
