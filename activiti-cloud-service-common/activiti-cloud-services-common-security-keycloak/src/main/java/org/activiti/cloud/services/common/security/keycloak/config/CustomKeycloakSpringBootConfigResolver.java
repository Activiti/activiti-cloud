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
package org.activiti.cloud.services.common.security.keycloak.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomKeycloakSpringBootConfigResolver implements KeycloakConfigResolver {

    private AdapterConfig adapterConfig;
    private KeycloakDeployment keycloakDeployment;

    @Autowired
    public CustomKeycloakSpringBootConfigResolver(AdapterConfig adapterConfig) {
        this.adapterConfig = adapterConfig;
    }

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        if (keycloakDeployment != null) {
            return keycloakDeployment;
        }

        keycloakDeployment = KeycloakDeploymentBuilder.build(adapterConfig);

        return keycloakDeployment;
    }
}
