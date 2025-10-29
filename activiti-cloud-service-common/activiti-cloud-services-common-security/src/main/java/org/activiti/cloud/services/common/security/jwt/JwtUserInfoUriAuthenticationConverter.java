/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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

import java.time.Instant;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtUserInfoUriAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUserInfoUriAuthenticationConverter.class);

    public static final String SESSION_ID_CLAIM = "sid";

    protected static final String SUBJECT_CLAIM = "sub";

    private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
    private ClientRegistration clientRegistration;
    private OAuth2UserServiceCacheable oAuth2UserServiceCacheable;
    protected String usernameClaim = "preferred_username";

    public JwtUserInfoUriAuthenticationConverter(
        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter,
        ClientRegistration clientRegistration,
        OAuth2UserServiceCacheable oAuth2UserServiceCacheable
    ) {
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
        this.clientRegistration = clientRegistration;
        this.oAuth2UserServiceCacheable = oAuth2UserServiceCacheable;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = this.jwtGrantedAuthoritiesConverter.convert(jwt);
        String principalClaimValue = getPrincipalClaimName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    public void setUsernameClaim(String usernameClaim) {
        this.usernameClaim = usernameClaim;
    }

    public String getPrincipalClaimName(Jwt jwt) {
        String username = jwt.getClaimAsString(usernameClaim);
        if (username == null) {
            String cacheKey = getCacheKey(jwt);
            if (cacheKey != null) {
                if (jwt.getClaimAsStringList("scope").contains("openid")) {
                    Instant issuedAt = jwt.getIssuedAt();
                    Instant expiresAt = jwt.getExpiresAt();
                    OAuth2AccessToken accessToken = new OAuth2AccessToken(
                        TokenType.BEARER,
                        jwt.getTokenValue(),
                        issuedAt,
                        expiresAt
                    );
                    OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);
                    OAuth2User oAuth2User = this.oAuth2UserServiceCacheable.loadUser(userRequest, cacheKey);
                    username = oAuth2User.getName();
                } else {
                    LOGGER.warn("Cannot load user data for {} because the token has not openid scope", cacheKey);
                    username = cacheKey;
                    OAuth2User oAuth2User = new DefaultOAuth2User(
                        jwtGrantedAuthoritiesConverter.convert(jwt),
                        jwt.getClaims(),
                        "sub"
                    );
                    oAuth2UserServiceCacheable.putUser(oAuth2User, cacheKey);
                }
            }
        }
        return username;
    }

    private String getCacheKey(Jwt jwt) {
        return jwt.hasClaim(SESSION_ID_CLAIM)
            ? jwt.getClaimAsString(SESSION_ID_CLAIM)
            : jwt.getClaimAsString(SUBJECT_CLAIM);
    }
}
