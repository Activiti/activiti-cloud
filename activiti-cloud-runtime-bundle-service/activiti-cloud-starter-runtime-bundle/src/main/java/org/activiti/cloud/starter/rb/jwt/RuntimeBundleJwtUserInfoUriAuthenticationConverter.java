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
package org.activiti.cloud.starter.rb.jwt;

import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

public class RuntimeBundleJwtUserInfoUriAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter;

    public RuntimeBundleJwtUserInfoUriAuthenticationConverter(JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter) {
        this.jwtUserInfoUriAuthenticationConverter = jwtUserInfoUriAuthenticationConverter;
    }

    /**
     * In Runtime Bundle's case, we need to perform the same conversion as in other modules using the common jwt converter,
     * but also adding the id of the currently authenticated user so that it can be set as the initiator of the process instance
     * @param jwt
     * @return the Jwt authentication token
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AbstractAuthenticationToken result = jwtUserInfoUriAuthenticationConverter.convert(jwt);
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(result.getName());
        return result;
    }
}
