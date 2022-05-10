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
package org.activiti.cloud.services.identity.keycloak.config;

import org.activiti.cloud.services.common.security.keycloak.config.CommonSecurityAutoConfiguration;
import org.activiti.cloud.services.identity.keycloak.KeycloakActivitiAuthenticationProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@Configuration
@AutoConfigureBefore(CommonSecurityAutoConfiguration.class)
public class RuntimeBundleSecurityAutoConfiguration {

    @Bean
    public AuthenticationProvider keycloakAuthenticationProvider(JwtDecoder jwtDecoder) {
        return new KeycloakActivitiAuthenticationProvider(jwtDecoder);
    }

}
