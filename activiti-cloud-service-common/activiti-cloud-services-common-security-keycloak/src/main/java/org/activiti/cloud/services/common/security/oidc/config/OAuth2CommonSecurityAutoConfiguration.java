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
package org.activiti.cloud.services.common.security.oidc.config;

import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.cloud.security.authorization.EnableAuthorizationConfiguration;
import org.activiti.cloud.services.common.security.oidc.OAuth2SecurityContextTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

//@Configuration
//@EnableAuthorizationConfiguration
//@PropertySource("classpath:keycloak-configuration.properties")

public class OAuth2CommonSecurityAutoConfiguration {

    @Bean
//    @ConditionalOnMissingBean
    public SecurityContextTokenProvider securityContextTokenProvider(OAuth2AuthorizedClientService auth2AuthorizedClientService) {
        return new OAuth2SecurityContextTokenProvider(auth2AuthorizedClientService);
    }

}
