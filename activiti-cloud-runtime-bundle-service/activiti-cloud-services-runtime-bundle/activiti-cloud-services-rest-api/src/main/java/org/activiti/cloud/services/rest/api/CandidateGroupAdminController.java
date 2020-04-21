package org.activiti.cloud.services.rest.api;

import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/admin/v1/tasks/{taskId}/candidate-groups",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface CandidateGroupAdminController {

    @RequestMapping(method = RequestMethod.POST)
    void addCandidateGroups(@PathVariable("taskId") String taskId,
                            @RequestBody CandidateGroupsPayload candidateGroupsPayload);

    @RequestMapping(method = RequestMethod.DELETE)
    void deleteCandidateGroups(@PathVariable("taskId") String taskId,
                               @RequestBody CandidateGroupsPayload candidateGroupsPayload);

    @RequestMapping(method = RequestMethod.GET)
    CollectionModel<EntityModel<CandidateGroup>> getGroupCandidates(@PathVariable("taskId") String taskId);

}
