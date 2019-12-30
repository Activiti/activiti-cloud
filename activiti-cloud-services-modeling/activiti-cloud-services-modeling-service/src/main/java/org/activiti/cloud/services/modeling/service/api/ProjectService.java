package org.activiti.cloud.services.modeling.service.api;

import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.service.ModelService.ProjectAccessControl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Business logic related to {@link Project} entities
 */
public interface ProjectService {
    Page<Project> getProjects(Pageable pageable,
                              String name);

    Project createProject(Project project);

    Project updateProject(Project projectToUpdate,
                          Project newProject);

    void deleteProject(Project project);

    Optional<Project> findProjectById(String projectId);

    FileContent exportProject(Project project) throws IOException;

    ProjectAccessControl getProjectAccessControl(Project project);

    Project importProject(MultipartFile file, @Nullable String name) throws IOException;

    void validateProject(Project project);
}
