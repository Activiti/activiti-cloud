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
package org.activiti.cloud.services.notifications.graphql.graphiql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GraphiQLAutoConfigurationTest {
    
    @Autowired
    private KeycloakJsonController keycloakJsonController;

    @Autowired
    private GraphiQLConfigController graphiQLConfigController;
    
    
    @SpringBootApplication
    static class Application {
        // 
    }
    
    @Test
    public void contextLoads() {
        assertThat(keycloakJsonController.get().getBody()).isNotNull();
        assertThat(graphiQLConfigController.getGraphQLWebPath()).isEqualTo("/default-app/graphql");
        assertThat(graphiQLConfigController.getGraphQLWsPath()).isEqualTo("/default-app/ws/graphql");
    }

    @Test
    public void testContextPath() {
        assertThat(graphiQLConfigController.appendSegmentToPath("","/graphql")).isEqualTo("/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath("/","/graphql")).isEqualTo("/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath(null,"/graphql")).isEqualTo("/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath("/default-app","/graphql")).isEqualTo("/default-app/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath("/default-app/","/graphql")).isEqualTo("/default-app/graphql");
        assertThat(graphiQLConfigController.appendSegmentToPath("/default-app","graphql")).isEqualTo("/default-app/graphql");
    }
    
    
}
