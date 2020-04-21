package org.activiti.cloud.services.rest.api;

import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping(value = "/v1/tasks/{taskId}/candidate-users",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
public interface CandidateUserController {

    @RequestMapping(method = RequestMethod.POST)
    void addCandidateUsers(@PathVariable("taskId") String taskId,
                           @RequestBody CandidateUsersPayload candidateUsersPayload);

    @RequestMapping(method = RequestMethod.DELETE)
    void deleteCandidateUsers(@PathVariable("taskId") String taskId,
                              @RequestBody CandidateUsersPayload candidateUsersPayload);

    @RequestMapping(method = RequestMethod.GET)
    CollectionModel<EntityModel<CandidateUser>> getUserCandidates(@PathVariable("taskId") String taskId);

}
