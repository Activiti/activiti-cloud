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

import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.impl.ProjectImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDecoratorServiceTest {

    @Mock
    ProjectDecorator projectDecorator1;
    @Mock
    ProjectDecorator projectDecorator2;

    ProjectDecoratorService projectDecoratorService;

    @BeforeEach
    void setUp() {
        when(projectDecorator1.decoratorName()).thenReturn("decorator1-name");
        when(projectDecorator2.decoratorName()).thenReturn("decorator2-name");
        projectDecoratorService = new ProjectDecoratorService(List.of(projectDecorator1, projectDecorator2));
    }

    @Test
    void should_decorateProjectsWithAllDecorators() {
        List<Project> projects = List.of(new ProjectImpl());
        projectDecoratorService.decorateAll(projects, List.of("decorator1-name", "decorator2-name"));
        verify(projectDecorator1).decorateAll(eq(projects));
        verify(projectDecorator2).decorateAll(eq(projects));
    }

    @Test
    void should_decorateProjectsWithOneDecorator() {
        List<Project> projects = List.of(new ProjectImpl());
        projectDecoratorService.decorateAll(projects, List.of("decorator1-name"));
        verify(projectDecorator1).decorateAll(eq(projects));
        verify(projectDecorator2, never()).decorateAll(any());
    }

    @Test
    void should_notDecorateProjects_when_differentDecoratorName() {
        List<Project> projects = List.of(new ProjectImpl());
        projectDecoratorService.decorateAll(projects, List.of("other-name"));
        verify(projectDecorator1, never()).decorateAll(any());
        verify(projectDecorator2, never()).decorateAll(any());
    }

    @Test
    void should_decorateSingleProjectWithAllDecorators() {
        Project project = new ProjectImpl();
        projectDecoratorService.decorate(project, List.of("decorator1-name", "decorator2-name"));
        verify(projectDecorator1).decorate(eq(project));
        verify(projectDecorator2).decorate(eq(project));
    }

    @Test
    void should_decorateSingleProjectWithOneDecorator() {
        Project project = new ProjectImpl();
        projectDecoratorService.decorate(project, List.of("decorator1-name"));
        verify(projectDecorator1).decorate(eq(project));
        verify(projectDecorator2, never()).decorate(any());
    }

    @Test
    void should_notDecorateSingleProject_when_differentDecoratorName() {
        Project project = new ProjectImpl();
        projectDecoratorService.decorate(project, List.of("other-name"));
        verify(projectDecorator1, never()).decorate(any());
        verify(projectDecorator2, never()).decorate(any());
    }

}
