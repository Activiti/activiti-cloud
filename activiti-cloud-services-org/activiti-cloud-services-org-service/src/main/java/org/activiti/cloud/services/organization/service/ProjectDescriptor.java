package org.activiti.cloud.services.organization.service;

import java.util.Date;
import java.util.Set;
import org.activiti.cloud.organization.api.Project;

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
