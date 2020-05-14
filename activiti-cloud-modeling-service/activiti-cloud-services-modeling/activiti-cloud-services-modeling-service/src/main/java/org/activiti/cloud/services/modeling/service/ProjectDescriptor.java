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
package org.activiti.cloud.services.modeling.service;

import java.util.Date;
import java.util.Set;
import org.activiti.cloud.modeling.api.Project;

public class ProjectDescriptor {

    private final Project project;
    private Set<String> users;
    private Set<String> groups;

    public ProjectDescriptor(Project project) {
        this.project = project;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public String getId() {
        return project.getId();
    }

    public String getName() {
        return project.getName();
    }

    public String getVersion() {
        return project.getVersion();
    }

    public String getDescription() {
        return project.getDescription();
    }

    public Object getCreatedBy() {
        return project.getCreatedBy();
    }

    public Date getCreationDate() {
        return project.getCreationDate();
    }

    public Object getLastModifiedBy() {
        return project.getLastModifiedBy();
    }

    public Date getLastModifiedDate() {
        return project.getLastModifiedDate();
    }

    public Project toProject(){
        return this.project;
    }

}
