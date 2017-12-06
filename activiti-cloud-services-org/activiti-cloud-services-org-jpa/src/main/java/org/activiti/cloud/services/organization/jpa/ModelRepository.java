package org.activiti.cloud.services.organization.jpa;

import org.activiti.cloud.organization.core.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * JPA Repository for {@link Model} entity
 */
@RepositoryRestResource(path = "models",
        collectionResourceRel = "models",
        itemResourceRel = "models")
public interface ModelRepository extends JpaRepository<Model, String> {
}
