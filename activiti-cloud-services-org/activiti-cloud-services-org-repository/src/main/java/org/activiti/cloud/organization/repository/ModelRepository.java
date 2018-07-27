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

package org.activiti.cloud.organization.repository;

import java.util.List;
import java.util.Optional;

import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for {@link Model} entities repository
 */
public interface ModelRepository<A extends Application, M extends Model<A, ?>> {

    Page<M> getTopLevelModels(Pageable pageable);

    Page<M> getModels(A application,
                      Pageable pageable);

    Optional<M> findModelById(String modelId);

    M createModel(M model);

    M updateModel(M modelToUpdate);

    void deleteModel(M model);

    default M createModel(A application,
                          M model) {
        model.setApplication(application);
        return createModel(model);
    }

    default M updateModel(M modelToUpdate,
                          M newModel) {
        modelToUpdate.setName(newModel.getName());
        modelToUpdate.setContentType(newModel.getContentType());
        modelToUpdate.setContent(newModel.getContent());
        return updateModel(modelToUpdate);
    }

    default Optional<M> findModelByLink(String link) {
        return findModelById(link.substring(link.lastIndexOf('/') + 1));
    }

    default void createModelsReference(A application,
                                       List<String> applicationsLinks) {
        applicationsLinks
                .stream()
                .distinct()
                .map(this::findModelByLink)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(model -> {
                    model.setApplication(application);
                    updateModel(model);
                });
    }
}
