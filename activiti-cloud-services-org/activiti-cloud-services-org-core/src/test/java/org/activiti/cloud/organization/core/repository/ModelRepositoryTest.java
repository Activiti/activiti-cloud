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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.organization.core.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.activiti.cloud.organization.core.model.Model.ModelType.PROCESS_MODEL;

/**
 * Tests for {@link ModelRepository}
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelRepositoryTest {

    @Spy
    private ModelRepository modelRepository;

    @Captor
    private ArgumentCaptor<Model> modelArgumentCaptor;

    @Test
    public void testCreateModel() {
        // GIVEN
        Project parentProject = new Project("parent_project_id",
                                      "Parent Project");
        Model childModel = new Model("child_model_id",
                                     "Child Model",
                                     PROCESS_MODEL,
                                     "child_model_id");

        // WHEN
        modelRepository.createModel(parentProject,
                                      childModel);

        // THEN
        verify(modelRepository,
               times(1))
                .createModel(modelArgumentCaptor.capture());

        Model createdModel = modelArgumentCaptor.getValue();
        assertThat(createdModel).isNotNull();
        assertThat(createdModel.getId()).isEqualTo("child_model_id");
        assertThat(createdModel.getProject()).isNotNull();
        assertThat(createdModel.getProject().getId()).isEqualTo("parent_project_id");
    }

    @Test
    public void testUpdateModel() {
        // GIVEN
        Model modelToUpdate = new Model("model_id",
                                        "Model Name",
                                        PROCESS_MODEL,
                                        "model_id");
        Model model = new Model("new_model_id",
                                "New Model Name",
                                PROCESS_MODEL,
                                "new_model_id");

        // WHEN
        modelRepository.updateModel(modelToUpdate,
                                      model);

        // THEN
        verify(modelRepository,
               times(1))
                .updateModel(modelArgumentCaptor.capture());

        Model updatedModel = modelArgumentCaptor.getValue();
        assertThat(updatedModel).isNotNull();
        assertThat(updatedModel.getId()).isEqualTo("model_id");
        assertThat(updatedModel.getData()).isNotNull();
        assertThat(updatedModel.getData().getModelId()).isEqualTo("new_model_id");
        assertThat(updatedModel.getData().getName()).isEqualTo("New Model Name");
    }

    @Test
    public void testFindModelByLink() {
        // GIVEN
        String modelSelfLink = "http://localhost:8080:/models/model_id";

        // WHEN
        modelRepository.findModelByLink(modelSelfLink);

        // THEN
        verify(modelRepository,
               times(1))
                .findModelById(eq("model_id"));
    }

    @Test
    public void testCreateSubmodelReference() {
        // GIVEN
        Project parentProject = new Project("parent_project_id",
                                      "Parent Project");

        Model model1 = new Model("model1",
                                        "Model 1",
                                        PROCESS_MODEL,
                                        "model1");

        Model model2 = new Model("model2",
                                 "Model 2",
                                 PROCESS_MODEL,
                                 "model2");

        List<String> submodelLinks = Arrays.asList(
                "http://localhost:8080/models/model1",
                "http://localhost:8080/models/wrong_model_id",
                "http://localhost:8080/models/model2"
        );

        doReturn(Optional.of(model1)).when(modelRepository).findModelById(eq("model1"));
        doReturn(Optional.of(model2)).when(modelRepository).findModelById(eq("model2"));

        // WHEN
        modelRepository.createModelsReference(parentProject,
                                                submodelLinks);

        // THEN
        verify(modelRepository,
               times(3))
                .findModelById(anyString());

        verify(modelRepository,
               times(2))
                .updateModel(modelArgumentCaptor.capture());

        List<Model> updatedModels = modelArgumentCaptor.getAllValues();
        assertThat(updatedModels).hasSize(2);

        assertThat(updatedModels
                           .stream()
                           .map(Model::getId)
                           .collect(Collectors.toList()))
                .containsExactly("model1",
                                 "model2");

        assertThat(updatedModels
                           .stream()
                           .map(Model::getProject)
                           .filter(Objects::nonNull)
                           .map(Project::getId)
                           .collect(Collectors.toList()))
                .containsExactly("parent_project_id",
                                 "parent_project_id");
    }
}
