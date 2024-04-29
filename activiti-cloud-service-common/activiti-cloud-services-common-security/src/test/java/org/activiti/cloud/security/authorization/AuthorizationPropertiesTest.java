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
package org.activiti.cloud.security.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

/*
The configuration below contains a mixed case for properties to check if it doesn't cause any issue
 */
@SpringBootTest(
    properties = {
        "authorizations.security-constraints[0].authRoles[0]=ACTIVITI_USER",
        "authorizations.securityConstraints[0].auth-roles[1]=ACTIVITI_ADMIN",
        "authorizations.security-constraints[0].security-collections[0].patterns[0]=/v1/*",
        "authorizations.security-constraints[0].securityCollections[0].patterns[1]=/v1/abc/*",
        "authorizations.security-constraints[0].securityCollections[1].patterns[0]=/v1/def/*",
        "authorizations.security-constraints[1].authRoles[0]=ACTIVITI_DEVOPS",
        "authorizations.security-constraints[1].securityCollections[0].patterns[0]=/v1/ghi/*",
        "authorizations.security-constraints[2].authPermissions[0]=permissionA",
        "authorizations.security-constraints[2].auth-permissions[1]=permissionB",
        "authorizations.security-constraints[2].securityCollections[0].patterns[0]=/v1/jkl/*",
    },
    classes = { AuthorizationProperties.class }
)
@EnableConfigurationProperties(value = AuthorizationProperties.class)
class AuthorizationPropertiesTest {

    @Autowired
    private AuthorizationProperties authorizationProperties;

    @Test
    public void configurationLoadTest() {
        assertEquals(3, authorizationProperties.getSecurityConstraints().size());
        assertEquals(2, authorizationProperties.getSecurityConstraints().get(0).getAuthRoles().length);
        assertEquals("ACTIVITI_USER", authorizationProperties.getSecurityConstraints().get(0).getAuthRoles()[0]);
        assertEquals("ACTIVITI_ADMIN", authorizationProperties.getSecurityConstraints().get(0).getAuthRoles()[1]);
        assertEquals(2, authorizationProperties.getSecurityConstraints().get(0).getSecurityCollections().length);
        assertEquals(
            2,
            authorizationProperties.getSecurityConstraints().get(0).getSecurityCollections()[0].getPatterns().length
        );
        assertEquals(
            1,
            authorizationProperties.getSecurityConstraints().get(0).getSecurityCollections()[1].getPatterns().length
        );
        assertEquals(
            "/v1/*",
            authorizationProperties.getSecurityConstraints().get(0).getSecurityCollections()[0].getPatterns()[0]
        );
        assertEquals(
            "/v1/abc/*",
            authorizationProperties.getSecurityConstraints().get(0).getSecurityCollections()[0].getPatterns()[1]
        );
        assertEquals(
            "/v1/def/*",
            authorizationProperties.getSecurityConstraints().get(0).getSecurityCollections()[1].getPatterns()[0]
        );
        assertEquals(1, authorizationProperties.getSecurityConstraints().get(1).getSecurityCollections().length);
        assertEquals(
            1,
            authorizationProperties.getSecurityConstraints().get(1).getSecurityCollections()[0].getPatterns().length
        );
        assertEquals("ACTIVITI_DEVOPS", authorizationProperties.getSecurityConstraints().get(1).getAuthRoles()[0]);
        assertEquals(
            "/v1/ghi/*",
            authorizationProperties.getSecurityConstraints().get(1).getSecurityCollections()[0].getPatterns()[0]
        );
        assertEquals("permissionA", authorizationProperties.getSecurityConstraints().get(2).getAuthPermissions()[0]);
        assertEquals("permissionB", authorizationProperties.getSecurityConstraints().get(2).getAuthPermissions()[1]);
        assertEquals(1, authorizationProperties.getSecurityConstraints().get(2).getSecurityCollections().length);
        assertEquals(
            1,
            authorizationProperties.getSecurityConstraints().get(2).getSecurityCollections()[0].getPatterns().length
        );
        assertEquals(
            "/v1/jkl/*",
            authorizationProperties.getSecurityConstraints().get(2).getSecurityCollections()[0].getPatterns()[0]
        );
    }
}
