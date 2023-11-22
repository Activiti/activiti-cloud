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
package org.activiti.cloud.starter.tests.swagger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@DirtiesContext
@AutoConfigureMockMvc
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@Import(TestChannelBinderConfiguration.class)
public class QuerySwaggerITSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * This is not a test. It's actually generating the swagger.json and yaml definition of the service. It is used by maven generate-swagger profile build.
     */
    @Test
    public void generateSwagger() throws Exception {
        ModelConverters.getInstance().addConverter(new HibernateBytecodeEnhancementIgnoredConverter());

        mockMvc
            .perform(get("/v3/api-docs/Query").accept(MediaType.APPLICATION_JSON))
            .andDo(result -> {
                JsonNode jsonNodeTree = objectMapper.readTree(result.getResponse().getContentAsByteArray());
                Files.write(
                    new File("target/swagger.json").toPath(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(jsonNodeTree)
                );
                Files.write(new File("target/swagger.yaml").toPath(), new YAMLMapper().writeValueAsBytes(jsonNodeTree));
            });
    }

    /**
     * API model schema filter to enable hiding of API model attributes.
     */
    public static class HibernateBytecodeEnhancementIgnoredConverter implements ModelConverter {

        private static final Set<Class<?>> IGNORED_CLASSES = Set.of(
            EntityEntry.class,
            ManagedEntity.class,
            PersistentAttributeInterceptor.class
        );

        @Override
        public Schema resolve(
            AnnotatedType annotatedType,
            ModelConverterContext context,
            Iterator<ModelConverter> chain
        ) {
            JavaType javaType = Json.mapper().constructType(annotatedType.getType());
            if (javaType != null && IGNORED_CLASSES.contains(javaType.getRawClass())) {
                return null;
            }
            return (chain.hasNext()) ? chain.next().resolve(annotatedType, context, chain) : null;
        }
    }
}
