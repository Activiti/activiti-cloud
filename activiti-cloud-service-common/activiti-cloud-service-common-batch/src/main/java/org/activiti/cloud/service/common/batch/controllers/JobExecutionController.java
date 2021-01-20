// Generated by delombok at Wed Jan 13 19:00:08 PST 2021
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
package org.activiti.cloud.service.common.batch.controllers;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Optional;

import org.activiti.cloud.service.common.batch.config.ActivitiCloudCommonBatchRestAutoConfiguration;
import org.activiti.cloud.service.common.batch.core.jobexecution.JobExecutionService;
import org.activiti.cloud.service.common.batch.domain.JobConfig;
import org.activiti.cloud.service.common.batch.resources.JobExecutionResource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/admin/batch/jobs/executions",
                produces = "application/hal+json")
@ConditionalOnProperty(name = ActivitiCloudCommonBatchRestAutoConfiguration.REST_API_ENABLED,
                       havingValue = "true",
                       matchIfMissing = true)
public class JobExecutionController {
    private final JobExecutionService jobExecutionService;

    public JobExecutionController(final JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @GetMapping("/{id}")
    public JobExecutionResource get(@PathVariable long id) {
        return new JobExecutionResource(jobExecutionService.jobExecution(id));
    }

    @GetMapping
    public CollectionModel<JobExecutionResource> all(@RequestParam(value = "jobName", required = false) String jobName, @RequestParam(value = "exitCode", required = false) String exitCode, @RequestParam(value = "limitPerJob", defaultValue = "3") Integer limitPerJob) {
        Collection<JobExecutionResource> jobExecutions = jobExecutionService.jobExecutions(optionalOrEmpty(jobName),
                                                                                           optionalOrEmpty(exitCode),
                                                                                           limitPerJob).stream()
                                                                                                       .map(JobExecutionResource::new)
                                                                                                       .collect(toList());

        return new CollectionModel<>(jobExecutions, linkTo(methodOn(JobExecutionController.class).all(jobName, exitCode, limitPerJob)).withSelfRel().expand());
    }

    @PostMapping
    public ResponseEntity<JobExecutionResource> put(@RequestBody JobConfig jobConfig) {
        JobExecutionResource resource = new JobExecutionResource(jobExecutionService.launch(jobConfig));
        boolean failed = resource.getJobExecution()
                                 .getExitCode()
                                 .equals(ExitStatus.FAILED.getExitCode());

        return new ResponseEntity<>(resource, failed ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK);
    }

    private Optional<String> optionalOrEmpty(String s) {
        if (s != null) {
            s = s.trim();
            if (s.isEmpty()) s = null;
        }
        return Optional.ofNullable(s);
    }
}
