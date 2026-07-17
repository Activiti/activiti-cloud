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
package org.activiti.cloud.services.query.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JsonViewsTest {

    private final JsonMapper objectMapper = new JsonMapper();

    @Test
    void should_notSerializeIntegrationContextsWithGeneralView() throws Exception {
        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity();
        serviceTaskEntity.setIntegrationContexts(List.of());

        JsonNode payload = objectMapper.readTree(
            objectMapper.writerWithView(JsonViews.General.class).writeValueAsString(serviceTaskEntity)
        );

        assertThat(payload.has("integrationContexts")).isFalse();
    }

    @Test
    void should_serializeIntegrationContextsWithIntegrationContextsView() throws Exception {
        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity();
        serviceTaskEntity.setIntegrationContexts(List.of());

        JsonNode payload = objectMapper.readTree(
            objectMapper.writerWithView(JsonViews.IntegrationContexts.class).writeValueAsString(serviceTaskEntity)
        );

        assertThat(payload.has("integrationContexts")).isTrue();
        assertThat(payload.get("integrationContexts").isArray()).isTrue();
    }
}
