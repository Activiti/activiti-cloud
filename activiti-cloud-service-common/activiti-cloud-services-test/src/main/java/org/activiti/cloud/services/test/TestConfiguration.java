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
package org.activiti.cloud.services.test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.services.test.identity.interceptor.IdentityTokenInterceptor;
import org.activiti.cloud.services.test.identity.keycloak.KeycloakTokenProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
@AutoConfigureBefore(value = RestTemplateAutoConfiguration.class)
public class TestConfiguration {

    private final List<JacksonModule> modules;

    public TestConfiguration(List<JacksonModule> modules) {
        this.modules = modules;
    }

    @Bean
    @ConditionalOnMissingBean
    public IdentityTokenProducer keycloakTokenProducer(
        @Value("${keycloak.auth-server-url:}") String authServerUrl,
        @Value("${keycloak.realm:}") String realm
    ) {
        return new KeycloakTokenProducer(authServerUrl, realm);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        value = "identity.test.token-interceptor.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public IdentityTokenInterceptor identityTokenInterceptor(IdentityTokenProducer keycloakTokenProducer) {
        return new IdentityTokenInterceptor(keycloakTokenProducer);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder(
        @Autowired(required = false) IdentityTokenInterceptor identityTokenInterceptor
    ) {
        JsonMapper.Builder builder = JsonMapper.builder().addModule(new HalJacksonModule());

        for (JacksonModule module : modules) {
            if (module.getModuleName().startsWith("map")) {
                builder.addModule(module);
            }
        }

        JsonMapper mapper = builder.build();

        JacksonJsonHttpMessageConverter jacksonHttpMessageConverter = new JacksonJsonHttpMessageConverter(mapper);
        jacksonHttpMessageConverter.setSupportedMediaTypes(
            Arrays.asList(MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON)
        );

        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder().additionalMessageConverters(
            jacksonHttpMessageConverter,
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new ByteArrayHttpMessageConverter()
        );
        if (identityTokenInterceptor != null) {
            return restTemplateBuilder.additionalInterceptors(identityTokenInterceptor);
        } else {
            return restTemplateBuilder;
        }
    }
}
