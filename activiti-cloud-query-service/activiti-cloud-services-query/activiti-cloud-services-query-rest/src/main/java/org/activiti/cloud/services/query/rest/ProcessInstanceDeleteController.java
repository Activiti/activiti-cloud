package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceRepresentationModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;

@ConditionalOnProperty(name = "activiti.rest.enable-deletion", matchIfMissing = true)
@RestController
@RequestMapping(
        value = "/admin/v1/process-instances",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceDeleteController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler;

    @Autowired
    public ProcessInstanceDeleteController(ProcessInstanceRepository processInstanceRepository,
                                           ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceRepresentationModelAssembler = processInstanceRepresentationModelAssembler;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public CollectionModel<EntityModel<CloudProcessInstance>> deleteProcessInstances (@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate) {

        Collection<EntityModel<CloudProcessInstance>> result = new ArrayList<>();
        Iterable <ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        for(ProcessInstanceEntity entity : iterable){
            result.add(processInstanceRepresentationModelAssembler.toModel(entity));
        }

        processInstanceRepository.deleteAll(iterable);

        return new CollectionModel<>(result);
    }







}
