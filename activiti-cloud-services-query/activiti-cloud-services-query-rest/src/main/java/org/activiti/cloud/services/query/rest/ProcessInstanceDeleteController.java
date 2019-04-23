package org.activiti.cloud.services.query.rest;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceResourceAssembler;
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
        value = "/admin/v1/process-instances",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceDeleteController {

    private final ProcessInstanceRepository processInstanceRepository;

    private ProcessInstanceResourceAssembler processInstanceResourceAssembler;

    @Autowired
    public ProcessInstanceDeleteController(ProcessInstanceRepository processInstanceRepository,
                                           ProcessInstanceResourceAssembler processInstanceResourceAssembler) {
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceResourceAssembler = processInstanceResourceAssembler;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Resources<Resource<CloudProcessInstance>> deleteProcessInstances (@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate) {

        Collection<Resource<CloudProcessInstance>> result = new ArrayList<>();
        Iterable <ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        for(ProcessInstanceEntity entity : iterable){
            result.add(processInstanceResourceAssembler.toResource(entity));
        }

        processInstanceRepository.deleteAll(iterable);

        return new Resources<>(result);
    }







}
