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

package org.activiti.cloud.services.modeling.service.utils;

import org.activiti.cloud.services.modeling.validation.project.ProjectNameValidator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectKeyGeneratorImplTest {

    private final ProjectKeyGeneratorImpl projectKeyGenerator = new ProjectKeyGeneratorImpl(new ProjectNameValidator());

    @CsvSource(
        value = {
            "project-name,project-name-",
            "project name,project-name-",
            "PROJECT NAME,project-name-",
            "   project    name   ,project-name-",
            "project!@#$%^&*()name,project-name-",
            "project veeeeery looooong name,project-veeeeery-loo-",
            "āăąćĉċčďđēĕėęěĝğġģĥi,aaaccccd-eeeeegggghi-",
        },
        ignoreLeadingAndTrailingWhitespace = false
    )
    @ParameterizedTest
    void should_generateKey(String projectName, String expectedKeyStart) {
        String key = projectKeyGenerator.generate(projectName);
        Assertions.assertThat(key).startsWith(expectedKeyStart).hasSize(expectedKeyStart.length() + 5);
    }

    @ValueSource(strings = { "这是书", "面ミ刊賛受治ほら", "!@#$%^&*()-", "" })
    @ParameterizedTest
    void should_generateKey(String projectName) {
        String key = projectKeyGenerator.generate(projectName);
        Assertions.assertThat(key).containsPattern("^[a-z0-9]{20}$");
    }
}
