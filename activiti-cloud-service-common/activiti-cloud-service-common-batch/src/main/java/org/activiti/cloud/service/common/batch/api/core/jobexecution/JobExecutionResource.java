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

package org.activiti.cloud.service.common.batch.api.core.jobexecution;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.RepresentationModel;

public class JobExecutionResource extends RepresentationModel<JobExecutionResource> {

    private JobExecution jobExecution;

    // For Jackson
    private JobExecutionResource() {
    }

    public JobExecutionResource(final JobExecution jobExecution) {
        this.jobExecution = jobExecution;
        add(linkTo(methodOn(JobExecutionController.class).get(jobExecution.getId())).withSelfRel());
    }

    public JobExecution getJobExecution() {
        return jobExecution;
    }
}
