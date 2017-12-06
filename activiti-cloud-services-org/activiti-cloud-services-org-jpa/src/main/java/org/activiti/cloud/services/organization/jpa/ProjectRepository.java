package org.activiti.cloud.services.organization.jpa;

import org.activiti.cloud.organization.core.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link Project} entity
 */
@RepositoryRestResource(path = "projects",
        collectionResourceRel = "projects",
        itemResourceRel = "projects")
public interface ProjectRepository extends JpaRepository<Project, String> {

}
