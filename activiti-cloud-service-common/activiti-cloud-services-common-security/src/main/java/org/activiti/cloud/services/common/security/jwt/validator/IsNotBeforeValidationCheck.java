package org.activiti.cloud.services.common.security.jwt.validator;

import org.springframework.security.oauth2.jwt.Jwt;

public class IsNotBeforeValidationCheck implements AbastractTimeValidationCheck {

    private final long offset;

    public IsNotBeforeValidationCheck(long offset) {
        this.offset = offset;
    }

    /**
     * The 'nbf' (NotBefore) claim is used by some auth providers as
     * a way to set the moment from which a token starts being valid.
     * A token could not be expired, but if its validity period has not started,
     * it would be still an invalid token
     * @param accessToken the Jwt access token
     * @return if the nbf claim is either in the past or the future
     */
    public boolean isValid(Jwt accessToken) {
        return accessToken.getNotBefore() == null ||
            currentTime(offset) >= accessToken.getNotBefore().toEpochMilli();
    }

}
