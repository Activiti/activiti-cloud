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

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * User authentication steps
 */
@EnableFeignContext
public class AuthenticationSteps {

    private static final String AUTH_TOKEN = "authToken";

    private static final String AUTH_CLIENT_ID = "activiti";
    private static final String AUTH_GRANT_TYPE = "password";
    private static final String AUTH_USERNAME = "hruser";
    private static final String AUTH_PASSWORD = "password";

    @Autowired
    private AuthenticationService authenticationService;

    @Step
    public void authenticateDefaultUser() throws IOException {
        AuthToken authToken = authenticationService
                .authenticate(AUTH_CLIENT_ID,
                              AUTH_GRANT_TYPE,
                              AUTH_USERNAME,
                              AUTH_PASSWORD);
        Serenity.setSessionVariable(AUTH_TOKEN).to(authToken);
    }

    @Step
    public void ensureUserIsAuthenticated() {
        AuthToken authToken = Serenity.sessionVariableCalled(AUTH_TOKEN);
        assertThat(authToken).isNotNull();
        assertThat(authToken.getAccess_token()).isNotNull();
    }
}
