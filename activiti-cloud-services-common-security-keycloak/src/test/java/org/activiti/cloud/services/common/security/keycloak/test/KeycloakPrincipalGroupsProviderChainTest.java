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

import org.activiti.api.runtime.shared.security.PrincipalGroupsProvider;
import org.activiti.cloud.services.common.security.keycloak.KeycloakPrincipalGroupsProviderChain;
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


public class KeycloakPrincipalGroupsProviderChainTest {
    
    private KeycloakPrincipalGroupsProviderChain subject;

    @Mock
    PrincipalGroupsProvider provider1;
    
    @Mock
    PrincipalGroupsProvider provider2;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        subject = new KeycloakPrincipalGroupsProviderChain(Arrays.asList(provider1, provider2));
    }

    @Test
    public void testGetGroups() {
        // given
        Principal principal = mock(KeycloakPrincipal.class);
        when(provider1.getGroups(Mockito.any())).thenReturn(null);
        when(provider2.getGroups(Mockito.any())).thenReturn(Arrays.asList("group1", 
                                                                          "group2"));
       
        // when
        List<String> result = subject.getGroups(principal);
        
        // then 
        assertThat(result).isNotEmpty()
                          .containsExactly("group1",
                                           "group2");
        
        verify(provider1).getGroups(ArgumentMatchers.eq(principal));
        verify(provider2).getGroups(ArgumentMatchers.eq(principal));
    }
    
    @Test
    public void testGetGropusSecurityException() {
        // given
        Principal principal = mock(KeycloakPrincipal.class);
        when(provider1.getGroups(Mockito.any())).thenReturn(null);
        when(provider2.getGroups(Mockito.any())).thenReturn(null);
       
        // when
        Throwable thrown = catchThrowable(() -> { subject.getGroups(principal); });
        
        // then
        assertThat(thrown).isInstanceOf(SecurityException.class);
        
        verify(provider1).getGroups(ArgumentMatchers.eq(principal));
        verify(provider2).getGroups(ArgumentMatchers.eq(principal));
        
    }    
}
