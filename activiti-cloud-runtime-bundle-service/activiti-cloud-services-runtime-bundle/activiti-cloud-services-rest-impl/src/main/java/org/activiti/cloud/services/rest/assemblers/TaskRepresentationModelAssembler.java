/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.assemblers;

import java.util.ArrayList;
import java.util.List;

import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import static org.activiti.api.task.model.Task.TaskStatus.ASSIGNED;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class TaskRepresentationModelAssembler implements RepresentationModelAssembler<Task, EntityModel<CloudTask>> {

    private ToCloudTaskConverter converter;

    public TaskRepresentationModelAssembler(ToCloudTaskConverter converter) {
        this.converter = converter;
    }

    @Override
    public EntityModel<CloudTask> toModel(Task task) {
        CloudTask cloudTask = converter.from(task);
        List<Link> links = new ArrayList<>();
        links.add(linkTo(methodOn(TaskControllerImpl.class).getTaskById(cloudTask.getId())).withSelfRel());
        if (ASSIGNED != cloudTask.getStatus()) {
            links.add(linkTo(methodOn(TaskControllerImpl.class).claimTask(cloudTask.getId())).withRel("claim"));
        } else {
            links.add(linkTo(methodOn(TaskControllerImpl.class).releaseTask(cloudTask.getId())).withRel("release"));
            links.add(linkTo(methodOn(TaskControllerImpl.class).completeTask(cloudTask.getId(),
                                                                         null)).withRel("complete"));
        }
        // standalone task are not bound to a process instance
        if (cloudTask.getProcessInstanceId() != null && !cloudTask.getProcessInstanceId().isEmpty()) {
            links.add(linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(cloudTask.getProcessInstanceId())).withRel("processInstance"));
        }
        if (cloudTask.getParentTaskId() != null && !cloudTask.getParentTaskId().isEmpty()) {
            links.add(linkTo(methodOn(TaskControllerImpl.class).getTaskById(cloudTask.getParentTaskId())).withRel("parent"));
        }
        links.add(linkTo(HomeControllerImpl.class).withRel("home"));
        return new EntityModel<>(cloudTask,
                              links);
    }

}
