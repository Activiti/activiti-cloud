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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.junit.jupiter.api.Test;

class DefaultModelExtensionsImportDecoratorTest {

    private final DefaultModelExtensionsImportDecorator defaultModelExtensionsImportDecorator =
        new DefaultModelExtensionsImportDecorator();

    @Test
    void should_decorateModel() {
        var model = getModel();
        defaultModelExtensionsImportDecorator.decorate(
            model,
            Map.of("extensions", Map.of("name", "ext name", "displayName", "ext name", "key", "ext-key"))
        );
        assertThat(model.getName()).isEqualTo("ext name");
        assertThat(model.getDisplayName()).isEqualTo("ext name");
        assertThat(model.getKey()).isEqualTo("ext-key");
    }

    @Test
    void should_notDecorateModelWithoutExtensions() {
        var model = getModel();
        defaultModelExtensionsImportDecorator.decorate(
            model,
            Map.of("sthElse", Map.of("name", "ext name", "displayName", "ext name", "key", "ext-key"))
        );
        assertThat(model.getName()).isEqualTo("name");
        assertThat(model.getDisplayName()).isEqualTo("name");
        assertThat(model.getKey()).isEqualTo("key");
    }

    private ModelImpl getModel() {
        var model = new ModelImpl();
        model.setName("name");
        model.setDisplayName("name");
        model.setKey("key");
        return model;
    }
}
