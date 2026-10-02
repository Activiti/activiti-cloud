/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.services.connectors.mtc;

public class MtcIntegrationRequestInterceptor {

    private final MtcProperties mtcProperties;

    public MtcIntegrationRequestInterceptor(MtcProperties mtcProperties) {
        this.mtcProperties = mtcProperties;
    }

    public boolean isMtcEnabled(String connectorType) {
        if (!mtcProperties.isEnabled()) {
            return false;
        }
        String templateType = resolveTemplateType(connectorType);
        if (templateType != null) {
            return mtcProperties.getConnectorTypes().contains(templateType);
        }
        return mtcProperties.getConnectorTypes().stream().anyMatch(connectorType::startsWith);
    }

    public String resolveMtcDestination(String connectorType) {
        String templateType = resolveTemplateType(connectorType);
        if (templateType != null) {
            String suffix = extractMethodSuffix(connectorType);
            return mtcProperties.getExchangePrefix() + templateType + suffix;
        }
        return mtcProperties.getExchangePrefix() + connectorType;
    }

    public String getBinderName() {
        return mtcProperties.getBinderName();
    }

    public String resolveConnectorType(String connectorType) {
        String templateType = resolveTemplateType(connectorType);
        if (templateType != null) {
            return templateType + extractMethodSuffix(connectorType);
        }
        return connectorType;
    }

    private String resolveTemplateType(String connectorType) {
        String baseKey = extractBaseKey(connectorType);
        return mtcProperties.getTypeMappings().get(baseKey);
    }

    private String extractBaseKey(String connectorType) {
        int dotIndex = connectorType.lastIndexOf('.');
        return dotIndex > 0 ? connectorType.substring(0, dotIndex) : connectorType;
    }

    private String extractMethodSuffix(String connectorType) {
        int dotIndex = connectorType.lastIndexOf('.');
        return dotIndex > 0 ? connectorType.substring(dotIndex) : "";
    }
}
