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
package org.activiti.services.connectors.recovery;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.engine.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resolves the connector template (e.g. {@code agenticTaskConnector}) of a service task.
 * The service task implementation ({@code <connectorKey>.<action>}) is looked up in the BPMN model,
 * then the connector definition with the matching key is read from the connectors directory of the
 * project release matching the process definition version: {@code <projectReleaseDir>/<version>/connectors/}.
 */
public class ConnectorTemplateResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorTemplateResolver.class);

    private final RepositoryService repositoryService;
    private final JsonMapper jsonMapper;
    private final ResourcePatternResolver resourceLoader;
    private final String projectReleaseLocation;

    private final Map<Integer, Map<String, String>> templatesByVersion = new HashMap<>();

    ConnectorTemplateResolver(
        RepositoryService repositoryService,
        JsonMapper jsonMapper,
        ResourcePatternResolver resourceLoader,
        String projectReleaseDir
    ) {
        this.repositoryService = repositoryService;
        this.jsonMapper = jsonMapper;
        this.resourceLoader = resourceLoader;
        this.projectReleaseLocation = toLocation(projectReleaseDir);
    }

    public Optional<String> resolveTemplate(String processDefinitionId, String flowNodeId) {
        var version = toVersion(processDefinitionId);
        if (version.isEmpty()) {
            LOGGER.warn("Unable to extract the version from process definition id {}.", processDefinitionId);
            return Optional.empty();
        }
        return findImplementation(processDefinitionId, flowNodeId)
            .map(ConnectorTemplateResolver::toConnectorKey)
            .map(connectorKey -> getTemplatesByConnectorKey(version.get()).get(connectorKey));
    }

    private Optional<String> findImplementation(String processDefinitionId, String flowNodeId) {
        var bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        if (bpmnModel == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bpmnModel.getFlowElement(flowNodeId))
            .filter(ServiceTask.class::isInstance)
            .map(ServiceTask.class::cast)
            .map(ServiceTask::getImplementation);
    }

    /**
     * Process definition ids have the form {@code <key>:<version>:<id>}.
     */
    static Optional<Integer> toVersion(String processDefinitionId) {
        var parts = processDefinitionId == null ? new String[0] : processDefinitionId.split(":");
        if (parts.length < 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(parts[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static String toConnectorKey(String implementation) {
        var actionSeparatorIndex = implementation.lastIndexOf('.');
        return actionSeparatorIndex < 0 ? implementation : implementation.substring(0, actionSeparatorIndex);
    }

    private static String toLocation(String projectReleaseDir) {
        var location = ResourceUtils.isUrl(projectReleaseDir)
            ? projectReleaseDir
            : ResourceUtils.FILE_URL_PREFIX + projectReleaseDir;
        return location.endsWith("/") ? location : location + "/";
    }

    private String connectorsLocation(int version) {
        return projectReleaseLocation + version + "/connectors/";
    }

    private synchronized Map<String, String> getTemplatesByConnectorKey(int version) {
        var templates = templatesByVersion.get(version);
        if (templates == null) {
            try {
                templates = loadTemplates(connectorsLocation(version));
                templatesByVersion.put(version, templates);
            } catch (IOException e) {
                // not cached: loading is retried on the next resolution
                LOGGER.error("Unable to load connector definitions from {}.", connectorsLocation(version), e);
                return Map.of();
            }
        }
        return templates;
    }

    private Map<String, String> loadTemplates(String connectorsLocation) throws IOException {
        Map<String, String> templates = new HashMap<>();
        if (!resourceLoader.getResource(connectorsLocation).exists()) {
            LOGGER.warn("Connectors directory {} not found.", connectorsLocation);
            return templates;
        }
        for (Resource resource : resourceLoader.getResources(connectorsLocation + "**.json")) {
            readTemplate(resource).ifPresent(entry -> templates.put(entry.getKey(), entry.getValue()));
        }
        return templates;
    }

    private Optional<Map.Entry<String, String>> readTemplate(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode connector = jsonMapper.readTree(inputStream);
            var key = connector.path("key").asString(null);
            var template = connector.path("template").asString(null);
            if (key == null || template == null) {
                return Optional.empty();
            }
            return Optional.of(Map.entry(key, template));
        } catch (Exception e) {
            LOGGER.warn("Unable to read connector definition {}.", resource.getDescription(), e);
            return Optional.empty();
        }
    }
}
