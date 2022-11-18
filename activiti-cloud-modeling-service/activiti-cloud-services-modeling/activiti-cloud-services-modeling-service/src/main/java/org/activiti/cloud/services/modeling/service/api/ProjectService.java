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
package org.activiti.cloud.services.modeling.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.service.api.ModelService.ProjectAccessControl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Business logic related to {@link Project} entities
 */
public interface ProjectService {
    Page<Project> getProjects(Pageable pageable, String name, List<String> filters, List<String> include);

    Project createProject(Project project);

    Project updateProject(Project projectToUpdate, Project newProject);

    void deleteProject(Project project);

    Optional<Project> findProjectById(String projectId, List<String> include);

    default Optional<Project> findProjectById(String projectId) {
        return findProjectById(projectId, null);
    }

    FileContent exportProject(Project project) throws IOException;

    Project copyProject(Project projectToCopy, String newProjectName);

    ProjectAccessControl getProjectAccessControl(Project project);

    Project importProject(MultipartFile file, @Nullable String name) throws IOException;

    Project importProject(final InputStream file, String name) throws IOException;

    void validateProject(Project project);

    Project replaceProjectContentWithProvidedModelsInFile(Project project, InputStream inputStream) throws IOException;
}
