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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

public class ProjectKeyGeneratorImpl implements ProjectKeyGenerator {

    public static final String KEY_SEPARATOR = "-";
    public static final String ALPHANUMERIC_REGEX = "[^a-z0-9]+";
    private final ProjectNameValidator projectNameValidator;

    public ProjectKeyGeneratorImpl(ProjectNameValidator projectNameValidator) {
        this.projectNameValidator = projectNameValidator;
    }

    //TODO improve on key generation
    @Override
    public String generate(String projectName) {
        if (StringUtils.isNotBlank(projectName)) {
            String sanitizedProjectName = StringUtils.substring(
                stripKeySeparators(
                    StringUtils
                        .stripAccents(projectName)
                        .toLowerCase()
                        .trim()
                        .replaceAll(ALPHANUMERIC_REGEX, KEY_SEPARATOR)
                ),
                0,
                20
            );
            if (StringUtils.isNotBlank(sanitizedProjectName)) {
                String generatedKey =
                    sanitizedProjectName + KEY_SEPARATOR + RandomStringUtils.randomAlphanumeric(5).toLowerCase();
                if (isValidProjectName(generatedKey)) {
                    return generatedKey;
                }
            }
        }
        return RandomStringUtils.randomAlphanumeric(20).toLowerCase();
    }

    private boolean isValidProjectName(String projectName) {
        return projectNameValidator.validateDNSName(projectName, "project").findAny().isEmpty();
    }

    private String stripKeySeparators(String string) {
        return StringUtils.stripEnd(StringUtils.stripStart(string, KEY_SEPARATOR), KEY_SEPARATOR);
    }
}
