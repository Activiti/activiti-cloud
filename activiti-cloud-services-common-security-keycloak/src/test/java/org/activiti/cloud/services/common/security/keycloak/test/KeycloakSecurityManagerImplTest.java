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

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.common.security.keycloak.KeycloakSecurityManagerImpl;
import org.activiti.cloud.services.common.security.keycloak.test.support.WithMockKeycloakUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class KeycloakSecurityManagerImplTest {
    
    @Autowired
    private SecurityManager securityManager;
    
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {
        
    }

    @Test
    public void contextLoads() {
        assertThat(securityManager).isInstanceOf(KeycloakSecurityManagerImpl.class);
    }

    @Test
    @WithMockKeycloakUser(username = "hruser")
    public void testGetAuthenticatedUserId() {
        // given
        
        // when
        String result = securityManager.getAuthenticatedUserId();
        
        // then
        assertThat(result).isEqualTo("hruser");
    }
    
    @Test
    @WithMockKeycloakUser(groups = {"hr", "admins"})
    public void testGetAuthenticatedUserGroups() {
        // given
        
        // when
        List<String> result = securityManager.getAuthenticatedUserGroups();
        
        // then
        assertThat(result).isNotEmpty()
                          .containsExactly("hr", "admins");
    }
    
    @Test
    @WithMockKeycloakUser(roles = {"ACTIVITI_USER"})
    public void testGetAuthenticatedUserRoles() {
        // given
        
        // when
        List<String> result = securityManager.getAuthenticatedUserRoles();
        
        // then
        assertThat(result).isNotEmpty()
                          .containsExactly("ACTIVITI_USER");
    }
    
    @Test
    public void testGetAuthenticatedUserIdAnonymous() {
        // given
        
        // when
        Throwable thrown = catchThrowable(() -> { securityManager.getAuthenticatedUserId(); });
        
        // then
        assertThat(thrown).isInstanceOf(SecurityException.class);
    }

    @Test
    public void testGetAuthenticatedUserGroupsAnonymous() {
        // given
        
        // when
        Throwable thrown = catchThrowable(() -> { securityManager.getAuthenticatedUserGroups(); });
        
        // then
        assertThat(thrown).isInstanceOf(SecurityException.class);
    }

    @Test
    public void testGetAuthenticatedUserRolesAnonymous() {
        // given
        
        // when
        Throwable thrown = catchThrowable(() -> { securityManager.getAuthenticatedUserRoles(); });
        
        // then
        assertThat(thrown).isInstanceOf(SecurityException.class);
        
    }    
    

}
