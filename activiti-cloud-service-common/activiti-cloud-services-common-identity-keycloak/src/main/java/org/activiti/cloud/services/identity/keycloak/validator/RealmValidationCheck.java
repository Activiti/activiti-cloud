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
        if (!realmUrl.equals(accessToken.getIssuer())) {
            LOGGER.error("Invalid token issuer. Expected '" + realmUrl + "', but was '" + accessToken.getIssuer() + "'");
            return false;
        }

        return true;
    }

    public String getRealmUrl() {
        return String.format("%s/realms/%s", authServerUrl, realm);
    }
}
