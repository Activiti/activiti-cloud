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

import feign.Feign;
import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.cloud.identity.config.IdentitySearchCacheConfiguration;
import org.activiti.cloud.security.feign.AuthTokenRequestInterceptor;
import org.activiti.cloud.security.feign.ClientCredentialsAuthRequestInterceptor;
import org.activiti.cloud.security.feign.configuration.ClientCredentialsAuthConfiguration;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.config.ActivitiKeycloakAutoConfiguration;
import org.activiti.cloud.services.test.identity.keycloak.KeycloakTokenProducer;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@Import({IdentitySearchCacheConfiguration.class, ActivitiKeycloakAutoConfiguration.class, ClientCredentialsAuthConfiguration.class})
public class KeycloakClientApplication {

    @Bean
    public SecurityContextTokenProvider securityContextTokenProvider(@Value("${keycloak.auth-server-url:}") String authServerUrl,
                                                                     @Value("${keycloak.realm:}") String realm,
                                                                     @Value("${keycloak.user:testuser}") String user) {
        return () -> Optional.of(new KeycloakTokenProducer(authServerUrl, realm)
                                     .withTestUser(user)
                                     .withTestPassword("password")
                                     .withResource("activiti")
                                     .getAccessTokenString());
    }


    @Bean
    @Primary
    public KeycloakClient keycloak(
        @Value("${keycloak.auth-server-url:}") String authServerUrl,
        @Value("${keycloak.auth-server-url}/admin/realms/${keycloak.realm}/") String url,
        ObjectFactory<HttpMessageConverters> messageConverters,
        ObjectProvider<HttpMessageConverterCustomizer> customizers,
        ClientCredentialsAuthRequestInterceptor clientCredentialsAuthRequestInterceptor,
        @Value("${keycloak.user:testuser}") String user) {
        KeycloakClient keycloakClient = Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new SpringEncoder(messageConverters))
            .decoder(new SpringDecoder(messageConverters, customizers))
            .requestInterceptor(new AuthTokenRequestInterceptor() {
                @Override
                public Optional<String> getToken() {
                    return Optional.of(new KeycloakTokenProducer(authServerUrl, "activiti")
                        .withTestUser(user)
                        .withTestPassword("password")
                        .withResource("activiti")
                        .getAccessTokenString());
                }
            })
            .target(KeycloakClient.class, url);
        return keycloakClient;
    }



	public static void main(String[] args) {
		SpringApplication.run(KeycloakClientApplication.class, args);
	}

}
