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
package org.activiti.cloud.services.identity.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource("classpath:keycloak-configuration.properties")
@ContextConfiguration(
    initializers = { KeycloakContainerApplicationInitializer.class }
)
public class KeycloakInstanceWrapperIT {

    @Autowired
    private KeycloakInstanceWrapper keycloakInstanceWrapper;

    @Test
    public void shouldWireWrapper() {
        assertThat(keycloakInstanceWrapper).isNotNull();
        assertThat(keycloakInstanceWrapper.getRealm()).isNotNull();
        assertThat(keycloakInstanceWrapper.getRealm().groups().groups())
            .isNotEmpty();
        assertThat(keycloakInstanceWrapper.getRealm().users().list())
            .isNotEmpty();
    }
}
