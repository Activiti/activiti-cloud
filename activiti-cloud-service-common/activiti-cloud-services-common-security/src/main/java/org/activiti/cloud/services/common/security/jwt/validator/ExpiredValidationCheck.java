package org.activiti.cloud.services.common.security.jwt.validator;

import org.springframework.security.oauth2.jwt.Jwt;

public class ExpiredValidationCheck implements AbastractTimeValidationCheck {

    private final long offset;

    public ExpiredValidationCheck(long offset) {
        this.offset = offset;
    }

    @Override
    public boolean isValid(Jwt accessToken) {
        return !(accessToken.getExpiresAt() != null &&
            accessToken.getExpiresAt().toEpochMilli() != 0 &&
            currentTime(offset) > accessToken.getExpiresAt().toEpochMilli());
    }

}
