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
package org.activiti.cloud.services.rest.api;

import org.activiti.api.process.model.payloads.CreateProcessInstancePayload;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/v1/process-instances", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface ProcessInstanceController {

    @RequestMapping(method = RequestMethod.GET)
    PagedResources<Resource<CloudProcessInstance>> getProcessInstances(Pageable pageable);

    @RequestMapping(method = RequestMethod.POST)
    Resource<CloudProcessInstance> startProcess(@RequestBody StartProcessPayload cmd);

    @RequestMapping(value = "/{processInstanceId}/start",method = RequestMethod.POST)
    Resource<CloudProcessInstance> startCreatedProcess(@PathVariable String processInstanceId,
                                                       @RequestBody(required = false) StartProcessPayload payload);

    @RequestMapping(value = "/create",method = RequestMethod.POST)
    Resource<CloudProcessInstance> createProcessInstance(@RequestBody CreateProcessInstancePayload cmd);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.GET)
    Resource<CloudProcessInstance> getProcessInstanceById(@PathVariable String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}/model",
            method = RequestMethod.GET,
            produces = "image/svg+xml")
    @ResponseBody
    String getProcessDiagram(@PathVariable String processInstanceId);

    @RequestMapping(value = "/signal", method = RequestMethod.POST)
    ResponseEntity<Void> sendSignal(@RequestBody SignalPayload signalPayload);

    @RequestMapping(value = "/message", method = RequestMethod.POST)
    Resource<CloudProcessInstance> sendStartMessage(@RequestBody StartMessagePayload startMessagePayload);

    @RequestMapping(value = "/message", method = RequestMethod.PUT)
    ResponseEntity<Void> receive(@RequestBody ReceiveMessagePayload receiveMessagePayload);

    @RequestMapping(value = "{processInstanceId}/suspend", method = RequestMethod.POST)
    Resource<CloudProcessInstance> suspend(@PathVariable String processInstanceId);

    @RequestMapping(value = "{processInstanceId}/resume", method = RequestMethod.POST)
    Resource<CloudProcessInstance> resume(@PathVariable String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.DELETE)
    Resource<CloudProcessInstance> deleteProcessInstance(@PathVariable String processInstanceId);

    @RequestMapping(value = "/{processInstanceId}", method = RequestMethod.PUT)
    Resource<CloudProcessInstance> updateProcess(@PathVariable("processInstanceId") String processInstanceId,
                                    @RequestBody UpdateProcessPayload payload);

    @RequestMapping(value = "/{processInstanceId}/subprocesses", method = RequestMethod.GET)
    PagedResources<Resource<CloudProcessInstance>> subprocesses(@PathVariable("processInstanceId") String processInstanceId,
                                                         Pageable pageable);
}
