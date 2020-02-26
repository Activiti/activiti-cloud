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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.activiti.api.runtime.shared.security.PrincipalRolesProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalRolesProviderChain;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;


public class KeycloakPrincipalRolesProviderChainTest {
    
    private KeycloakPrincipalRolesProviderChain subject;

    @Mock
    PrincipalRolesProvider provider1;
    
    @Mock
    PrincipalRolesProvider provider2;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        subject = new KeycloakPrincipalRolesProviderChain(Arrays.asList(provider1, provider2));
    }

    @Test
    public void testGetRoles() {
        // given
        Principal principal = mock(KeycloakPrincipal.class);
        when(provider1.getRoles(Mockito.any())).thenReturn(null);
        when(provider2.getRoles(Mockito.any())).thenReturn(Arrays.asList("role1", 
                                                                         "role2"));
       
        // when
        List<String> result = subject.getRoles(principal);
        
        // then 
        assertThat(result).isNotEmpty()
                          .containsExactly("role1",
                                           "role2");
        
        verify(provider1).getRoles(ArgumentMatchers.eq(principal));
        verify(provider2).getRoles(ArgumentMatchers.eq(principal));
        
    }    
    
    @Test
    public void testGetRolesSecurityException() {
        // given
        Principal principal = mock(KeycloakPrincipal.class);
        when(provider1.getRoles(Mockito.any())).thenReturn(null);
        when(provider2.getRoles(Mockito.any())).thenReturn(null);
       
        // when
        Throwable thrown = catchThrowable(() -> { subject.getRoles(principal); });
        
        // then
        assertThat(thrown).isInstanceOf(SecurityException.class);
        
        verify(provider1).getRoles(ArgumentMatchers.eq(principal));
        verify(provider2).getRoles(ArgumentMatchers.eq(principal));
    }
    
}
