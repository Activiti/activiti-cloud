package org.activiti.cloud.services.modeling.service.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.Task;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContent;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.services.common.file.FileContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

/**
 * Business logic related to {@link Model} entities
 */
public interface ModelService {

    List<Model> getAllModels(Project project);

    Page<Model> getModels(Project project,
                          ModelType modelType,
                          Pageable pageable);

    Model buildModel(String type,
                     String name);

    Model createModel(Project project,
                      Model model);

    Model updateModel(Model modelToBeUpdated,
                      Model newModel);

    void deleteModel(Model model);

    Optional<Model> findModelById(String modelId);

    Optional<FileContent> getModelExtensionsFileContent(Model model);

    void cleanModelIdList();

    Optional<FileContent> getModelDiagramFile(String modelId);

    String getExtensionsFilename(Model model);

    FileContent getModelContentFile(Model model);

    FileContent exportModel(Model model);

    Model updateModelContent(Model modelToBeUpdate,
                             FileContent fileContent);

    FileContent overrideModelContentId(Model model,
                                       FileContent fileContent);

    Optional<ModelContent> createModelContentFromModel(Model model,
                                                       FileContent fileContent);

    Model importSingleModel(Project project,
                            ModelType modelType,
                            FileContent fileContent);

    Model importModel(Project project,
                      ModelType modelType,
                      FileContent fileContent);

    Model importModelFromContent(Project project,
                                 ModelType modelType,
                                 FileContent fileContent);

    <T extends Task> List<T> getTasksBy(Project project,
                                        ModelType processModelType,
                                        @NonNull Class<T> clazz);

    List<Process> getProcessesBy(Project project, ModelType type);

    Model convertContentToModel(ModelType modelType,
                                FileContent fileContent);

    Model createModelFromContent(ModelType modelType,
                                 FileContent fileContent);

    Optional<String> contentFilenameToModelName(String filename,
                                                ModelType modelType);

    void validateModelContent(Model model,
                              ValidationContext validationContext);

    void validateModelContent(Model model,
                              FileContent fileContent);

    void validateModelContent(Model model,
                              FileContent fileContent,
                              ValidationContext validationContext);

    void validateModelExtensions(Model model,
                                 ValidationContext validationContext);

    void validateModelExtensions(Model model,
                                 FileContent fileContent);

    void validateModelExtensions(Model model,
                                 FileContent fileContent,
                                 ValidationContext validationContext);

    public static class ProjectAccessControl {

        private final Set<String> users;
        private final Set<String> groups;

        public ProjectAccessControl(Set<String> users, Set<String> groups) {
            this.users = users;
            this.groups = groups;
        }

        public Set<String> getGroups() {
            return groups;
        }

        public Set<String> getUsers() {
            return users;
        }
    }
}
