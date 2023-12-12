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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.activiti.cloud.modeling.api.ConnectorModelType;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.activiti.cloud.services.modeling.service.ProjectHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ModelExtensionsImportDecoratorServiceTest {

    private final DefaultModelExtensionsImportDecorator defaultModelExtensionsImportDecorator = Mockito.mock(
        DefaultModelExtensionsImportDecorator.class
    );

    private final ModelExtensionsImportDecorator mockDecorator = Mockito.mock(ModelExtensionsImportDecorator.class);

    private ModelExtensionsImportDecoratorService modelExtensionsImportDecoratorService;

    @BeforeEach
    void setUp() {
        when(mockDecorator.getHandledModelTypes()).thenReturn(Set.of(new ConnectorModelType()));
        modelExtensionsImportDecoratorService =
            new ModelExtensionsImportDecoratorService(
                List.of(defaultModelExtensionsImportDecorator, mockDecorator),
                defaultModelExtensionsImportDecorator
            );
    }

    @Test
    void should_callModelDecorator() {
        var model = new ModelImpl();
        model.setType(new ConnectorModelType().getName());
        modelExtensionsImportDecoratorService.decorate(model, new ProjectHolder());
        verify(mockDecorator).decorate(eq(model), any(ProjectHolder.class));
    }

    @Test
    void should_callDefaultDecorator_when_noDecoratorFoundForModelType() {
        var model = new ModelImpl();
        model.setType(new ProcessModelType().getName());
        modelExtensionsImportDecoratorService.decorate(model, new ProjectHolder());
        verify(defaultModelExtensionsImportDecorator).decorate(eq(model), any(ProjectHolder.class));
        verify(mockDecorator, never()).decorate(eq(model), any(ProjectHolder.class));
    }
}
