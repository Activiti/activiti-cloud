/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.rest.assemblers;

import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;

public class GroupCandidatesResourceAssembler implements ResourceAssembler<CandidateGroup, Resource<CandidateGroup>> {

    private String taskId;

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public Resource<CandidateGroup> toResource(CandidateGroup groupCandidates) {
        return new Resource<>(groupCandidates);
    }
}
