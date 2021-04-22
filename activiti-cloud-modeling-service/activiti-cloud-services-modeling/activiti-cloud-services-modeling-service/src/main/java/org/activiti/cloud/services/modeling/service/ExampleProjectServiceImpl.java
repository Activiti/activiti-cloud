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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.activiti.cloud.modeling.api.templates.ExampleProject;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.services.modeling.service.api.ExampleProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Business logic related to ExampleProject entities
 */
public class ExampleProjectServiceImpl implements ExampleProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ExampleProjectServiceImpl.class);

    private static final String LIST_FIELD_NAME = "list";
    private static final String ENTRIES_FIELD_NAME = "entries";
    private static final String ENTRY_FIELD_NAME = "entry";
    private static final String TEMP_FILE_PREFIX = "template";
    private static final String TEMP_FILE_SUFFIX = "tmp";

    private final JsonConverter<ExampleProject> jsonConverter;
    private final String templatesEndpoint;
    private final RestTemplate restTemplate;

    @Autowired
    public ExampleProjectServiceImpl(String templatesEndpoint, JsonConverter<ExampleProject> jsonConverter, RestTemplate restTemplate) {
        this.templatesEndpoint = templatesEndpoint;
        this.jsonConverter = jsonConverter;
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<File> getExampleProjectContentById(String exampleProjectId) {
        Optional<ExampleProject> result = getExampleProjects().stream().filter(exampleProject -> exampleProject.getId().equals(exampleProjectId)).findAny();
        if (!result.isPresent()) {
            return Optional.empty();
        }
        return restTemplate.execute(result.get().getExtensions().getContent().getUrl(), HttpMethod.GET, null, clientHttpResponse -> {
            File tmpFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(tmpFile));
            return Optional.of(tmpFile);
        });
    }

    @Override public List<ExampleProject> getExampleProjects() {
        try {
            ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(templatesEndpoint, JsonNode.class);
            if (Objects.isNull(responseEntity) || !responseEntity.hasBody()) {
                return Collections.emptyList();
            } else {
                return StreamSupport.stream(responseEntity.getBody().get(LIST_FIELD_NAME).get(ENTRIES_FIELD_NAME).spliterator(), false)
                        .map(jsonNode -> jsonConverter.convertToEntity(jsonNode.get(ENTRY_FIELD_NAME).toString())).collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Obtaining example projects from {} failed due to error: {}", templatesEndpoint, e.getMessage());
            return Collections.emptyList();
        }
    }

}
