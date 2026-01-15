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
package org.activiti.cloud.services.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.activiti.cloud.security.authorization.SecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(classes = { SecurityTestConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JwtUserInfoUriAuthenticationConverterTest {

    @MockitoSpyBean
    private OAuth2UserServiceCacheable oAuth2UserServiceCacheable;

    @MockitoBean
    private OAuth2UserService oAuth2UserService;

    @Autowired
    private CaffeineCacheManager caffeineCacheManager;

    @Autowired
    @Qualifier("jwtAuthenticationConverter")
    private JwtUserInfoUriAuthenticationConverter jwtUserInfoUriAuthenticationConverter;

    @Test
    void should_notTryToLoadUser_whenOpenidScopeIsMissing() {
        String sub = "abc-123";
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString(JwtUserInfoUriAuthenticationConverter.USERNAME_CLAIM)).thenReturn(null);
        when(jwt.hasClaim(JwtUserInfoUriAuthenticationConverter.SESSION_ID_CLAIM)).thenReturn(false);
        when(jwt.getClaimAsString(JwtUserInfoUriAuthenticationConverter.SUBJECT_CLAIM)).thenReturn(sub);
        when(jwt.getClaimAsStringList("scope")).thenReturn(List.of());
        when(jwt.getClaims()).thenReturn(Map.of(JwtUserInfoUriAuthenticationConverter.SUBJECT_CLAIM, sub));

        String principal = jwtUserInfoUriAuthenticationConverter.getPrincipalClaimName(jwt);
        assertThat(principal).isEqualTo(sub);

        ArgumentCaptor<OAuth2User> oAuth2UserCaptor = ArgumentCaptor.forClass(OAuth2User.class);
        verify(oAuth2UserServiceCacheable).putUser(oAuth2UserCaptor.capture(), eq(sub));
        assertThat(oAuth2UserCaptor.getValue().getName()).isEqualTo(sub);

        //verify that user is cached and no call to underlying service is made
        assertThat(oAuth2UserServiceCacheable.loadUser(mock(OAuth2UserRequest.class), sub).getName())
            .isEqualTo(principal);
        verifyNoInteractions(oAuth2UserService);
    }
}
