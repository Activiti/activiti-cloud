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

package org.activiti.cloud.qa.steps;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.rest.TokenHolder;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * User authentication steps
 */
@EnableFeignContext
public class AuthenticationSteps {

    private static final String AUTH_CLIENT_ID = "activiti";
    private static final String AUTH_GRANT_TYPE = "password";
    private static final String AUTH_USERNAME_TESTUSER = "testuser";
    private static final String AUTH_USERNAME_HRUSER = "hruser";
    private static final String AUTH_USERNAME_HRADMIN = "hradmin";
    private static final String AUTH_PASSWORD = "password";

    @Autowired
    private AuthenticationService authenticationService;

    @Step
    public void authenticateTestUser() {
        AuthToken authToken = authenticationService
                    .authenticate(AUTH_CLIENT_ID,
                            AUTH_GRANT_TYPE,
                            AUTH_USERNAME_TESTUSER,
                            AUTH_PASSWORD);
        TokenHolder.setAuthToken(authToken);
    }

    @Step
    public void authenticateHrUser() {
        AuthToken authToken = authenticationService
                .authenticate(AUTH_CLIENT_ID,
                        AUTH_GRANT_TYPE,
                        AUTH_USERNAME_HRUSER,
                        AUTH_PASSWORD);
        TokenHolder.setAuthToken(authToken);
    }

    @Step
    public void authenticateHrAdmin() {
        AuthToken authToken = authenticationService
                .authenticate(AUTH_CLIENT_ID,
                        AUTH_GRANT_TYPE,
                        AUTH_USERNAME_HRADMIN,
                        AUTH_PASSWORD);
        TokenHolder.setAuthToken(authToken);
    }

    @Step
    public void ensureUserIsAuthenticated() {
        AuthToken authToken = TokenHolder.getAuthToken();
        assertThat(authToken).isNotNull();
        assertThat(authToken.getAccess_token()).isNotNull();
    }
}
