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
package org.activiti.cloud.services.notifications.graphql.graphiql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class GraphiQLAutoConfigurationTest {

    @MockitoBean
    private BuildProperties buildProperties;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private KeycloakJsonController keycloakJsonController;

    @Autowired
    private GraphiQLConfigController graphiQLConfigController;

    @Autowired
    private GraphiQLIndexController graphiQLIndexController;

    @SpringBootApplication
    @EnableAutoConfiguration(exclude = { SpringDocConfiguration.class })
    static class Application {
        //
    }

    @Test
    void contextLoads() {
        assertThat(keycloakJsonController.get().getBody()).isNotNull();
        assertThat(graphiQLConfigController.getGraphQLWebPath()).isEqualTo("/default-app/graphql");
        assertThat(graphiQLConfigController.getGraphQLWsPath()).isEqualTo("/default-app/ws/graphql");
    }

    @Test
    void testContextPath() {
        assertThat(graphiQLConfigController.appendSegmentToPath("", "/graphql")).isEqualTo("/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath("/", "/graphql")).isEqualTo("/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath(null, "/graphql")).isEqualTo("/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath("/default-app", "/graphql")).isEqualTo(
            "/default-app/graphql"
        );
        assertThat(graphiQLConfigController.appendSegmentToPath("/default-app/", "/graphql")).isEqualTo(
            "/default-app/graphql"
        );
        assertThat(graphiQLConfigController.appendSegmentToPath("/default-app", "graphql")).isEqualTo(
            "/default-app/graphql"
        );
    }

    @Test
    void testGraphiQLIndexController() {
        assertThat(graphiQLIndexController.getIndex()).isEqualTo("forward:/graphiql/graphiql.html");
    }
}
