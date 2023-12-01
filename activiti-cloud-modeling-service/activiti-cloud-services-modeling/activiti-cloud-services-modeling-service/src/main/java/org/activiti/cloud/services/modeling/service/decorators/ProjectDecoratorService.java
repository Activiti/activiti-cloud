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

package org.activiti.cloud.services.modeling.service.decorators;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.activiti.cloud.modeling.api.Project;
import org.apache.commons.lang3.StringUtils;

public class ProjectDecoratorService {

    private final Map<String, ProjectDecorator> projectDecorators;

    public ProjectDecoratorService(List<ProjectDecorator> projectDecorators) {
        this.projectDecorators =
            projectDecorators
                .stream()
                .collect(Collectors.toMap(ProjectDecorator::decoratorName, projectDecorator -> projectDecorator));
    }

    public void decorate(Project project, List<String> decoratorNames) {
        decorate(decoratorNames, projectDecorator -> projectDecorator.decorate(project));
    }

    public void decorateAll(List<Project> projects, List<String> decoratorNames) {
        decorate(decoratorNames, projectDecorator -> projectDecorator.decorateAll(projects));
    }

    private void decorate(List<String> decoratorNames, Consumer<ProjectDecorator> projectDecoratorConsumer) {
        decoratorNames
            .stream()
            .filter(StringUtils::isNotBlank)
            .map(String::toLowerCase)
            .map(projectDecorators::get)
            .filter(Objects::nonNull)
            .forEach(projectDecoratorConsumer);
    }
}
