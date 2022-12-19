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
package org.activiti.cloud.services.modeling.validation.process;

public enum ServiceTaskImplementationType {

    EMAIL_SERVICE("email-service."),
    DOCGEN_SERVICE("docgen-service."),
    CONTENT_SERVICE("content-service."),
    HXP_CONTENT_SERVICE("hxp-content-service."),
    SCRIPT_TASK("script."),
    DMN_TASK("dmn-connector.");

    private String prefix;

    ServiceTaskImplementationType(final String type) {
        this.prefix = type;
    }

    public String getPrefix() {
        return prefix;
    }
}
