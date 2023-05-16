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
package org.activiti.cloud.starter.tests.monitoring;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.activiti.cloud.services.common.security.test.support.WithActivitiMockUser;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.containers.RabbitMQContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(
    initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class }
)
@DirtiesContext
public class ActuatorHealthIndicatorsIT {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithActivitiMockUser(username = "admin", roles = "ACTIVITI_ADMIN", groups = "admins")
    public void should_displayActuatorHealthIndicatorsDetails_when_authorized() throws Exception {
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        assertThatJson(result.getResponse().getContentAsString())
            .node("status")
            .isEqualTo("UP")
            .node("components.db.status")
            .isEqualTo("UP")
            .node("components.rabbit.status")
            .isEqualTo("UP");
    }

    @Test
    @WithActivitiMockUser(username = "user", roles = "ACTIVITI_USER", groups = "users")
    public void should_notDisplayActuatorHealthIndicatorsDetails_when_notAuthorized() throws Exception {
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        assertThatJson(result.getResponse().getContentAsString())
            .node("status")
            .isEqualTo("UP")
            .node("components.db.status")
            .isAbsent()
            .node("components.rabbit.status")
            .isAbsent();
    }

    @Test
    public void should_notDisplayActuatorHealthIndicatorsDetails_when_notAuthenticated() throws Exception {
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        assertThatJson(result.getResponse().getContentAsString())
            .node("status")
            .isEqualTo("UP")
            .node("components.db.status")
            .isAbsent()
            .node("components.rabbit.status")
            .isAbsent();
    }
}
