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
package org.activiti.cloud.services.identity.keycloak;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.cloud.services.identity.keycloak.validator.PublicKeyValidationCheck;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = { KeycloakClientApplication.class },
    properties = { "keycloak.realm=activiti", "keycloak.use-resource-role-mappings=false" }
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
class PublicKeyValidationCheckIT {

    @Autowired
    private PublicKeyValidationCheck publicKeyValidationCheck;

    @Value("${keycloak.auth-server-url}")
    String authServerUrl;

    @Value("${keycloak.realm}")
    String realm;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    SecurityContextTokenProvider securityContextTokenProvider;

    @Test
    void should_tokenBeValid() {
        Jwt accessToken = Jwt
            .withTokenValue(securityContextTokenProvider.getCurrentToken().get())
            .header("a", "b")
            .claim("a", "b")
            .build();
        assertTrue(publicKeyValidationCheck.isValid(accessToken));
    }

    @Test
    void should_tokenBeInvalid_when_TokenIsWrong() throws Exception {
        String token =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJzWlFuWFQ2NTk3Q3U3SWttMF9VR1FXLUd6OE9nODVqYXNhTGpldHgxbnRvIn0.eyJleHAiOjE2NzI3NjM3NDEsImlhdCI6MTY3Mjc2MzQ0MSwianRpIjoiNDU3MTJkN2YtZGIyNC00NWQ0LWE3NjItM2I0ZWNjMWMwZTlhIiwiaXNzIjoiaHR0cHM6Ly9hcGFkZXYuZW52YWxmcmVzY28uY29tL2F1dGgvcmVhbG1zL2FsZnJlc2NvIiwiYXVkIjpbImx3bHNrIiwic3VicHJvY2Vzc2FwcCIsInJlYWxtLW1hbmFnZW1lbnQiLCJhY2ljaG9uIiwiYWNpY2giLCJjb25uZWN0b3ItZTJlIiwiYWNjb3VudCIsImNhbmRpZGF0ZWJhc2VhcHAiLCJlMmUtYmUtNDY2NzI5MTQiLCJzaW1wbGVhcHAiXSwic3ViIjoiODBlNzhlMmUtYjljMi00YmJjLThlNjYtMzc5ZWE0ZTYzNTIxIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYWN0aXZpdGkiLCJzZXNzaW9uX3N0YXRlIjoiNTg3NDljMjYtMDRlYi00NzY1LWE3MDQtNzcwMDNiY2UwM2M4IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjQyMDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIkFDVElWSVRJX01PREVMRVIiLCJvZmZsaW5lX2FjY2VzcyIsIkFDVElWSVRJX0RFVk9QUyIsIkFDVElWSVRJX0lERU5USVRZIiwiQUNUSVZJVElfQURNSU4iLCJBQ1RJVklUSV9BTkFMWVRJQ1MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Imx3bHNrIjp7InJvbGVzIjpbIkFDVElWSVRJX0FETUlOIl19LCJzdWJwcm9jZXNzYXBwIjp7InJvbGVzIjpbIkFDVElWSVRJX0FETUlOIl19LCJyZWFsbS1tYW5hZ2VtZW50Ijp7InJvbGVzIjpbIm1hbmFnZS1yZWFsbSIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyIsInF1ZXJ5LXVzZXJzIl19LCJhY2ljaG9uIjp7InJvbGVzIjpbIkFDVElWSVRJX0FETUlOIl19LCJhY2ljaCI6eyJyb2xlcyI6WyJBQ1RJVklUSV9BRE1JTiJdfSwiY29ubmVjdG9yLWUyZSI6eyJyb2xlcyI6WyJBQ1RJVklUSV9BRE1JTiJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19LCJjYW5kaWRhdGViYXNlYXBwIjp7InJvbGVzIjpbIkFDVElWSVRJX0FETUlOIl19LCJlMmUtYmUtNDY2NzI5MTQiOnsicm9sZXMiOlsiQUNUSVZJVElfQURNSU4iXX0sInNpbXBsZWFwcCI6eyJyb2xlcyI6WyJBQ1RJVklUSV9BRE1JTiJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsInNpZCI6IjU4NzQ5YzI2LTA0ZWItNDc2NS1hNzA0LTc3MDAzYmNlMDNjOCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IlN1cGVyIEFkbWluIFVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzdXBlcmFkbWludXNlciIsImdpdmVuX25hbWUiOiJTdXBlciBBZG1pbiIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoic3VwZXJhZG1pbnVzZXJAZXhhbXBsZS5jb20ifQ.JbRTxAJXhlqrrOnUD6Dk_rBf92dE13PuE-ZwReGM0_boXOT5Qiw62ErJvZNjJ3dks7oFcVNsuTdqhl5iyZ5Kbe3qPSYe-qi3T4eU2T0QmmW9jm7ZFpR-WqQb_1KhXAsxj9FB50GZOJFEB_1kU-kQ5Nu_6LnZiqJqT4Spi4QTH337uQFlIxUEWEBLT_CzvXfREfiSqASheZyHQVUTu6WbxI22EiL1jkVL-ReXCSHBSd9LcXSsxplARF97tHNdoF5J7wBU9g583Boq0TGidF0YM_cZ2ZJV7vDKVcMOUtkpML29Nuvalsg3Vm2gfS7giQ_OMEbT6WJKwZlNoYhzKKXKJA";

        Jwt accessToken = Jwt.withTokenValue(token).header("a", "b").claim("a", "b").build();
        assertFalse(publicKeyValidationCheck.isValid(accessToken));
    }
}
