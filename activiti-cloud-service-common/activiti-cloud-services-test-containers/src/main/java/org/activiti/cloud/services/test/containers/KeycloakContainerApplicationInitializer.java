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
package org.activiti.cloud.services.test.containers;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class KeycloakContainerApplicationInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final boolean IS_CI_ENVIRONMENT = System.getenv("CI") != null ||
                                                   System.getenv("GITHUB_ACTIONS") != null ||
                                                   System.getenv("JENKINS_URL") != null;

    private static final KeycloakContainer keycloakContainer = createKeycloakContainer();

    private static KeycloakContainer createKeycloakContainer() {
        KeycloakContainer container = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.3")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile("activiti-realm.json");

        // Disable reuse in CI environments to ensure proper cleanup
        if (!IS_CI_ENVIRONMENT) {
            container.withReuse(true);
        }

        return container;
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        initialize();
        TestPropertyValues.of(getContainerProperties()).applyTo(context.getEnvironment());

        // Add shutdown hook to ensure container stops in CI environments
        if (IS_CI_ENVIRONMENT) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (keycloakContainer != null && keycloakContainer.isRunning()) {
                    keycloakContainer.stop();
                }
            }));
        }
    }

    public void initialize() {
        if (!keycloakContainer.isRunning()) {
            keycloakContainer.start();
        }
    }

    public static KeycloakContainer getContainer() {
        return keycloakContainer;
    }

    public static String[] getContainerProperties() {
        return new String[] {
            "keycloak.auth-server-url=" + getAuthServerUrl(),
            "activiti.keycloak.client-id=activiti-keycloak",
            "activiti.keycloak.client-secret=545bc187-f10f-41f9-8d5f-cfca3dbada9c",
            "activiti.keycloak.grant-type=client_credentials",
        };
    }

    @NotNull
    private static String getAuthServerUrl() {
        String authServerUrl = keycloakContainer.getAuthServerUrl();
        if (authServerUrl.endsWith("/")) {
            return authServerUrl.substring(0, authServerUrl.length() - 1);
        } else {
            return authServerUrl;
        }
    }
}
