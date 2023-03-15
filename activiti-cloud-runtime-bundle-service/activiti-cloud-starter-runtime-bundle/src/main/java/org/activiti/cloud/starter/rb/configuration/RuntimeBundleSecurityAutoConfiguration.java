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
package org.activiti.cloud.starter.rb.configuration;

import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.starter.rb.jwt.RuntimeBundleJwtUserInfoUriAuthenticationConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class RuntimeBundleSecurityAutoConfiguration {

    private final Converter<Jwt, AbstractAuthenticationToken> jwtUserInfoUriAuthenticationConverter;

    @Autowired
    public RuntimeBundleSecurityAutoConfiguration(
        Converter<Jwt, AbstractAuthenticationToken> jwtUserInfoUriAuthenticationConverter
    ) {
        this.jwtUserInfoUriAuthenticationConverter = jwtUserInfoUriAuthenticationConverter;
    }

    @Primary
    @Bean("runtimeBundleJwtUserInfoUriAuthenticationConverter")
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new RuntimeBundleJwtUserInfoUriAuthenticationConverter(
            (JwtUserInfoUriAuthenticationConverter) jwtUserInfoUriAuthenticationConverter
        );
    }
}
