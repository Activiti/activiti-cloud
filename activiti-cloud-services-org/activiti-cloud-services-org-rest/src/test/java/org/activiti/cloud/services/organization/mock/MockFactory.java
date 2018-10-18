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

package org.activiti.cloud.services.organization.mock;

import java.util.UUID;

import org.activiti.cloud.services.organization.entity.ApplicationEntity;
import org.activiti.cloud.services.organization.entity.ModelEntity;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;

/**
 * Mocks factory
 */
public class MockFactory {

    public static ApplicationEntity application(String name) {
        return new ApplicationEntity(id(),
                                     name);
    }

    public static ModelEntity processModel(String name) {
        return new ModelEntity(id(),
                               name,
                               PROCESS);
    }

    public static ModelEntity processModelWithContent(String name,
                                                      String content) {
        ModelEntity processModel = new ModelEntity(id(),
                                                   name,
                                                   PROCESS);
        processModel.setContent(content);
        return processModel;
    }

    public static String id() {
        return UUID.randomUUID().toString();
    }
}
