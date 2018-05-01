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

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.controllers.HomeControllerImpl;
import org.activiti.cloud.services.rest.controllers.ProcessInstanceControllerImpl;
import org.activiti.cloud.services.rest.controllers.TaskControllerImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.ASSIGNED;

@Component
public class TaskResourceAssembler extends ResourceAssemblerSupport<Task, TaskResource> {

    public TaskResourceAssembler() {
        super(TaskControllerImpl.class,
              TaskResource.class);
    }

    @Override
    public TaskResource toResource(Task task) {
        List<Link> links = new ArrayList<>();
        links.add(linkTo(methodOn(TaskControllerImpl.class).getTaskById(task.getId())).withSelfRel());
        if (ASSIGNED != task.getStatus()) {
            links.add(linkTo(methodOn(TaskControllerImpl.class).claimTask(task.getId())).withRel("claim"));
        } else {
            links.add(linkTo(methodOn(TaskControllerImpl.class).releaseTask(task.getId())).withRel("release"));
            links.add(linkTo(methodOn(TaskControllerImpl.class).completeTask(task.getId(),
                                                                         null)).withRel("complete"));
        }
        // standalone tasks are not bound to a process instance
        if (task.getProcessInstanceId() != null && !task.getProcessInstanceId().isEmpty()) {
            links.add(linkTo(methodOn(ProcessInstanceControllerImpl.class).getProcessInstanceById(task.getProcessInstanceId())).withRel("processInstance"));
        }
        if (task.getParentTaskId() != null && !task.getParentTaskId().isEmpty()) {
            links.add(linkTo(methodOn(TaskControllerImpl.class).getTaskById(task.getParentTaskId())).withRel("parent"));
        }
        links.add(linkTo(HomeControllerImpl.class).withRel("home"));
        return new TaskResource(task,
                                links);
    }

    @Override
    public List<TaskResource> toResources(Iterable<? extends Task> entities) {
        return super.toResources(entities);
    }
}
