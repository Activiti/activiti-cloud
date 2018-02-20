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

package org.activiti.cloud.qa.user;

import java.io.IOException;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.AuthToken;
import org.activiti.cloud.qa.service.AuthenticationService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User session steps
 */
public class UserSessionSteps {

    @Step
    public void authenticateDefaultUser() throws IOException {
        AuthToken authToken = AuthenticationService.authenticate();
        Serenity.setSessionVariable("authToken").to(authToken);
    }

    @Step
    public void ensureUserIsAuthenticated() {
        AuthToken authToken = Serenity.sessionVariableCalled("authToken");
        assertThat(authToken).isNotNull();
        assertThat(authToken.getAccess_token()).isNotNull();
    }

}
