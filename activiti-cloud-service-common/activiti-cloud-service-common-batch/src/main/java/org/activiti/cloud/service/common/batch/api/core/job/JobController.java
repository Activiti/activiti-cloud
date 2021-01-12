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
package org.activiti.cloud.service.common.batch.api.core.job;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;

import org.activiti.cloud.service.common.batch.api.core.SpringBatchRestCoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@ConditionalOnProperty(name = SpringBatchRestCoreAutoConfiguration.REST_API_ENABLED,
        havingValue = "true",
        matchIfMissing = true)
@RestController
@RequestMapping(value = "/jobs",
        produces = "application/hal+json")
public class JobController {

    @Autowired
    private JobService jobService;

    @Operation(summary = "Get a Spring Batch job by name")
    @GetMapping("/{jobName}")
    public JobResource get(@PathVariable String jobName) {
        return new JobResource(jobService.job(jobName));
    }

    @Operation(summary = "Get all Spring Batch jobs")
    @GetMapping
    public CollectionModel<JobResource> all() {
        Collection<JobResource> jobs = jobService.jobs().stream().map(JobResource::new).collect(toList());
        return new CollectionModel<>(jobs, linkTo(methodOn(JobController.class).all()).withSelfRel());
    }
}
