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

package org.activiti.cloud.services.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.identity.keycloak.KeycloakProperties;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starters.test.MyProducer;
import org.activiti.cloud.starters.test.StreamProducer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.MessageChannel;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@AutoConfigureBefore(value=RestTemplateAutoConfiguration.class)
@EnableBinding(StreamProducer.class)
public class TestConfiguration {

    private final List<Module> modules;

    public TestConfiguration(List<Module> modules) {
        this.modules = modules;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MyProducer myProducer(MessageChannel producer) {
        return new MyProducer(producer);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public KeycloakTokenProducer keycloakTokenProducer(KeycloakProperties keycloakProperties) {
        return new KeycloakTokenProducer(keycloakProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder(KeycloakTokenProducer keycloakTokenProducer) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                         false);
        
        mapper.registerModule(new Jackson2HalModule());
        
        for (Module module : modules) {
            if (module.getModuleName().startsWith("map")) {
                mapper.registerModule(module);
            }
        }

        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jackson2HttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON));
        jackson2HttpMessageConverter.setObjectMapper(mapper);

        return new RestTemplateBuilder().additionalMessageConverters(
                jackson2HttpMessageConverter,
                new StringHttpMessageConverter(StandardCharsets.UTF_8)).additionalInterceptors(keycloakTokenProducer);
    }
    
}
