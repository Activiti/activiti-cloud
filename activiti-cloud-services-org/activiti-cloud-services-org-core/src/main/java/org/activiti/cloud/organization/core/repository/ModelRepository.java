/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.organization.core.repository;

import java.util.List;
import java.util.Optional;

import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for {@link Model} entities repository
 */
public interface ModelRepository {

    Page<Model> getTopLevelModels(Pageable pageable);

    Page<Model> getModels(Project project,
                          Pageable pageable);

    Optional<Model> findModelById(String modelId);

    Model createModel(Model model);

    Model updateModel(Model modelToUpdate);

    void deleteModel(Model model);

    default Model createModel(Project project,
                              Model model) {
        model.setProject(project);
        return createModel(model);
    }

    default Model updateModel(Model modelToUpdate,
                              Model newModel) {
        modelToUpdate.setData(newModel.getData());
        return updateModel(modelToUpdate);
    }

    default Optional<Model> findModelByLink(String link) {
        return findModelById(link.substring(link.lastIndexOf('/') + 1));
    }

    default void createModelsReference(Project project,
                                       List<String> projectsLinks) {
        projectsLinks
                .stream()
                .distinct()
                .map(this::findModelByLink)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(model -> {
                    model.setProject(project);
                    updateModel(model);
                });
    }
}
