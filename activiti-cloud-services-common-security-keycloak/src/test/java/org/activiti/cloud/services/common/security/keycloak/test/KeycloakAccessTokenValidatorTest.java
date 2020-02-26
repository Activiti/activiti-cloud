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

package org.activiti.cloud.services.common.security.keycloak.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.activiti.cloud.services.common.security.keycloak.KeycloakAccessTokenValidator;
import org.junit.Test;
import org.keycloak.representations.AccessToken;


public class KeycloakAccessTokenValidatorTest {
    
    private KeycloakAccessTokenValidator subject = new KeycloakAccessTokenValidator() {};

    @Test
    public void testIsValidActive() {
        // given
        AccessToken accessToken = mock(AccessToken.class);
        
        when(accessToken.isActive()).thenReturn(true);
        
        // when
        Boolean result = subject.isValid(accessToken);
        
        // then
        assertThat(result).isTrue();
    }

    @Test
    public void testIsValidNotActive() {
        // given
        AccessToken accessToken = mock(AccessToken.class);
        
        when(accessToken.isActive()).thenReturn(false);
        
        // when
        Boolean result = subject.isValid(accessToken);
        
        // then
        assertThat(result).isFalse();
    }

    @Test
    public void testIsValidNull() {
        // given
        AccessToken accessToken = null;
        
        // when
        Throwable result = catchThrowable(() -> { subject.isValid(accessToken); });
        
        // then
        assertThat(result).isInstanceOf(SecurityException.class);
    }
    
}
