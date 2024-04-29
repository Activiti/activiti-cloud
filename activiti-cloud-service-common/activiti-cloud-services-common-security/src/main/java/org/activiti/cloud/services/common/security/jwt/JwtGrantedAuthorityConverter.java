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
package org.activiti.cloud.services.common.security.jwt;

import java.util.ArrayList;
import java.util.Collection;
import org.activiti.cloud.security.authorization.CustomAuthorizationManager;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtGrantedAuthorityConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtAccessTokenProvider jwtAccessTokenProvider;

    public JwtGrantedAuthorityConverter(JwtAccessTokenProvider jwtAccessTokenProvider) {
        this.jwtAccessTokenProvider = jwtAccessTokenProvider;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String role : jwtAccessTokenProvider.accessToken(jwt).getRoles()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(CustomAuthorizationManager.ROLE_PREFIX + role));
        }
        for (String permission : jwtAccessTokenProvider.accessToken(jwt).getPermissions()) {
            grantedAuthorities.add(
                new SimpleGrantedAuthority(CustomAuthorizationManager.PERMISSION_PREFIX + permission)
            );
        }
        return grantedAuthorities;
    }
}
