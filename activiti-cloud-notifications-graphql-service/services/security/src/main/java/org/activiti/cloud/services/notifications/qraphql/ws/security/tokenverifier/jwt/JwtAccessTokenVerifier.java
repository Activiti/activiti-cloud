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
package org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.jwt;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.activiti.cloud.services.common.security.jwt.JwtAccessTokenValidator;
import org.activiti.cloud.services.common.security.jwt.JwtUserInfoUriAuthenticationConverter;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessToken;
import org.activiti.cloud.services.notifications.qraphql.ws.security.tokenverifier.GraphQLAccessTokenVerifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtAccessTokenVerifier implements GraphQLAccessTokenVerifier {

    private final JwtAccessTokenValidator jwtAccessTokenValidator;
    private final JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter;
    private final JwtDecoder jwtDecoder;
    private final Function<Jwt, List<String>> rolesSupplier;

    public JwtAccessTokenVerifier(
        JwtAccessTokenValidator jwtAccessTokenValidator,
        JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter,
        JwtDecoder jwtDecoder,
        Function<Jwt, List<String>> rolesSupplier
    ) {
        this.jwtAccessTokenValidator = jwtAccessTokenValidator;
        this.jwtUserInfoUriAuthenticationConverter = jwtUserInfoUriAuthenticationConverter;
        this.jwtDecoder = jwtDecoder;
        this.rolesSupplier = rolesSupplier;
    }

    @Override
    public GraphQLAccessToken verifyToken(String tokenString) {
        Jwt jwt = jwtDecoder.decode(tokenString);
        if (jwtAccessTokenValidator.isValid(jwt)) {
            JwtAuthenticationToken accessToken = (JwtAuthenticationToken) jwtUserInfoUriAuthenticationConverter.convert(
                jwt
            );
            return new GraphQLAccessToken(accessToken.getName(), Set.copyOf(rolesSupplier.apply(jwt)), accessToken);
        } else {
            throw new BadCredentialsException("Invalid JWT token");
        }
    }
}
