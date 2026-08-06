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
package org.activiti.cloud.services.query.rest.metrics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint that returns the top N process variable groups
 * consuming the most storage. The number of results is configurable
 * via the {@code limit} query parameter (defaults to 10, capped at 100).
 */
@RestController
@RequestMapping(value = "/admin/v1/db-storage-consumers", produces = MediaType.APPLICATION_JSON_VALUE)
public class StorageConsumersController {

    private static final Logger log = LoggerFactory.getLogger(StorageConsumersController.class);

    private final StorageConsumersService storageConsumersService;

    @Autowired
    public StorageConsumersController(StorageConsumersService storageConsumersService) {
        this.storageConsumersService = storageConsumersService;
    }

    @GetMapping
    public Map<String, Object> storageConsumers(
        @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> consumers = storageConsumersService.findTopConsumers(limit);
            result.put("topConsumers", consumers);
            result.put("limit", storageConsumersService.resolveLimit(limit));
        } catch (Exception e) {
            log.error("Failed to query storage consumers", e);
            result.put("topConsumers", List.of());
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Repository-based endpoint that queries both process and task variable
     * tables, with process definition details (name, version).
     */
    @GetMapping("/variables")
    public Map<String, Object> storageConsumerVariables(
        @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> consumers = storageConsumersService.findTopStorageConsumers(limit);
            result.put("topConsumers", consumers);
            result.put("limit", storageConsumersService.resolveLimit(limit));
        } catch (Exception e) {
            log.error("Failed to query storage consumer variables", e);
            result.put("topConsumers", List.of());
            result.put("error", e.getMessage());
        }
        return result;
    }
}
