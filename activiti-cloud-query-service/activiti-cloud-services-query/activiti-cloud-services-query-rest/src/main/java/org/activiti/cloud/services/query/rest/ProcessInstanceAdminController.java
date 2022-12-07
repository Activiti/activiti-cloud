/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.types.Predicate;
import java.util.Arrays;
import java.util.List;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.query.model.JsonViews;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessInstanceRepresentationModelAssembler;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceQueryBody;
import org.activiti.cloud.services.query.rest.predicate.StatusProcessInstanceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/admin/v1/process-instances",
        produces = {
                MediaTypes.HAL_JSON_VALUE,
                MediaType.APPLICATION_JSON_VALUE
        })
public class ProcessInstanceAdminController {

    private final ProcessInstanceAdminService processInstanceAdminService;

    private ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler;

    private AlfrescoPagedModelAssembler<ProcessInstanceEntity> pagedCollectionModelAssembler;

    @Autowired
    public ProcessInstanceAdminController(ProcessInstanceAdminService processInstanceAdminService,
                                          ProcessInstanceRepresentationModelAssembler processInstanceRepresentationModelAssembler,
                                          AlfrescoPagedModelAssembler<ProcessInstanceEntity> pagedCollectionModelAssembler) {
        this.processInstanceAdminService = processInstanceAdminService;
        this.processInstanceRepresentationModelAssembler = processInstanceRepresentationModelAssembler;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(method = RequestMethod.GET, params = "!variableKeys")
    public PagedModel<EntityModel<CloudProcessInstance>> findAll(@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate,
                                                                  Pageable pageable) {
        return pagedCollectionModelAssembler.toModel(pageable,
            processInstanceAdminService.findAll(predicate, pageable),
            processInstanceRepresentationModelAssembler);
    }

    @JsonView(JsonViews.ProcessVariables.class)
    @RequestMapping(method = RequestMethod.GET, params = "variableKeys")
    public PagedModel<EntityModel<CloudProcessInstance>> findAllWithVariables(@QuerydslPredicate(root = ProcessInstanceEntity.class) Predicate predicate,
        @RequestParam(value = "variableKeys", required = false, defaultValue = "") List<String> variableKeys,
        Pageable pageable) {
        return pagedCollectionModelAssembler.toModel(pageable,
            processInstanceAdminService.findAllWithVariables(predicate, variableKeys, pageable),
            processInstanceRepresentationModelAssembler);
    }

    @RequestMapping(method = RequestMethod.POST)
    public MappingJacksonValue findAllFromBody(@RequestBody ProcessInstanceQueryBody queryBody, Pageable pageable) {

        PagedModel<EntityModel<CloudProcessInstance>> pagedModel = pagedCollectionModelAssembler.toModel(pageable,
            processInstanceAdminService.findAllFromBody(queryBody.getVariableKeys(),
                Arrays.asList(new StatusProcessInstanceFilter(queryBody.getStatus())), pageable),
            processInstanceRepresentationModelAssembler);

        MappingJacksonValue result = new MappingJacksonValue(pagedModel);
        if(queryBody.hasVariableKeys()) {
            result.setSerializationView(JsonViews.ProcessVariables.class);
        } else {
            result.setSerializationView(JsonViews.General.class);
        }

        return result;
    }

    @JsonView(JsonViews.General.class)
    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    public EntityModel<CloudProcessInstance> findById(@PathVariable String processInstanceId) {

        return processInstanceRepresentationModelAssembler.toModel(processInstanceAdminService.findById(processInstanceId));
    }

}
