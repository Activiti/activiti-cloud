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

package org.activiti.cloud.services.modeling.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.modeling.api.impl.ModelImpl;
import org.junit.jupiter.api.Test;

class ImportedModelTest {

    @Test
    void hasIdentifiersToUpdate_shouldReturnTrue_when_originalIdAndUpdatedIdAreSet() {
        ImportedModel importedModel = new ImportedModel(new ModelImpl(), "from", "to");

        assertThat(importedModel.hasIdentifiersToUpdate()).isTrue();
    }

    @Test
    void hasIdentifiersToUpdate_shouldReturnFalse_when_originalIdIsNotSet() {
        ImportedModel importedModel = new ImportedModel(new ModelImpl(), null, "any");

        assertThat(importedModel.hasIdentifiersToUpdate()).isFalse();
    }

    @Test
    void hasIdentifiersToUpdate_shouldReturnFalse_when_UpdateIdIsNotSet() {
        ImportedModel importedModel = new ImportedModel(new ModelImpl(), "from", null);

        assertThat(importedModel.hasIdentifiersToUpdate()).isFalse();
    }
}
