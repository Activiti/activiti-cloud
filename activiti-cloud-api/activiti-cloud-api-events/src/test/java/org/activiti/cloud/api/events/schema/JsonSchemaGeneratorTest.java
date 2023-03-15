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
package org.activiti.cloud.api.events.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.activiti.cloud.api.events.CloudRuntimeEventType;
import org.activiti.cloud.api.model.shared.impl.conf.CloudCommonModelAutoConfiguration;
import org.activiti.cloud.api.process.model.impl.conf.CloudProcessModelAutoConfiguration;
import org.activiti.cloud.api.task.model.impl.conf.CloudTaskModelAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
    classes = {
        JsonSchemaGeneratorTest.Config.class,
        CloudProcessModelAutoConfiguration.class,
        CloudTaskModelAutoConfiguration.class,
        CloudCommonModelAutoConfiguration.class,
    }
)
public class JsonSchemaGeneratorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaGeneratorTest.class);

    @Configuration
    public static class Config {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public CloudRuntimeEventRegistry cloudRuntimeEventRegistry() {
            return new CloudRuntimeEventRegistry();
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CloudRuntimeEventRegistry cloudEventRegistry;

    @Test
    public void cloudEventRegistryShouldContainAllEnumValues() {
        Set<String> eventTypeStringSet = EnumSet
            .allOf(CloudRuntimeEventType.class)
            .stream()
            .map(Enum::name)
            .collect(Collectors.toSet());

        assertThat(cloudEventRegistry.buildRegistry().keySet()).containsAll(eventTypeStringSet);
    }

    @Test
    public void generateSchema() {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);
        File outputDir = new File("target/schema/");
        outputDir.mkdirs();
        cloudEventRegistry
            .buildRegistry()
            .forEach((k, v) -> {
                try {
                    JsonSchema jsonSchema = schemaGen.generateSchema(v);
                    objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(new File(outputDir, k + ".json"), jsonSchema);
                } catch (Exception e) {
                    LOGGER.error("unable to generate schema for " + k, e);
                }
            });
    }
}
