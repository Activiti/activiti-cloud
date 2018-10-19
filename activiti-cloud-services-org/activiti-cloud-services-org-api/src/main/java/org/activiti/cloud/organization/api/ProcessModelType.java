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

package org.activiti.cloud.organization.api;

import org.springframework.stereotype.Component;

/**
 * Process model type
 */
@Component
public class ProcessModelType implements ModelType {

    public static final String PROCESS = "PROCESS";

    public static final String PROCESSES = "processes";

    public static final String BPMN20_XML = "bpmn20.xml";

    public static final String BPMN_XML = "bpmn.xml";

    public static final String BPMN = "bpmn";

    public static final String[] ALLOWED_FILE_EXTENSIONS = new String[]{BPMN20_XML, BPMN_XML, BPMN};

    @Override
    public String getName() {
        return PROCESS;
    }

    @Override
    public String getFolderName() {
        return PROCESSES;
    }

    @Override
    public String getContentFileExtension() {
        return BPMN20_XML;
    }

    @Override
    public String[] getAllowedContentFileExtension() {
        return ALLOWED_FILE_EXTENSIONS;
    }
}
