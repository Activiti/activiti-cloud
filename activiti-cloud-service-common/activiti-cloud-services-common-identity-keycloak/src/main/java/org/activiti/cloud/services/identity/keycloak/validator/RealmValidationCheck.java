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
package org.activiti.cloud.services.identity.keycloak.validator;

import org.activiti.cloud.services.common.security.jwt.validator.ValidationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

public class RealmValidationCheck implements ValidationCheck {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RealmValidationCheck.class);
    private String authServerUrl;
    private final String realm;

    public RealmValidationCheck(String authServerUrl, String realm) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
    }

    @Override
    public boolean isValid(Jwt accessToken) {
        String realmUrl = this.getRealmUrl();
        if (accessToken.getIssuer() != null && !realmUrl.equals(accessToken.getIssuer().toString())) {
            LOGGER.error("Invalid token issuer. Expected '" + realmUrl + "', but was '" + accessToken.getIssuer() + "'");
            return false;
        }

        return true;
    }

    public String getRealmUrl() {
        return String.format("%s/realms/%s", authServerUrl, realm);
    }
}
