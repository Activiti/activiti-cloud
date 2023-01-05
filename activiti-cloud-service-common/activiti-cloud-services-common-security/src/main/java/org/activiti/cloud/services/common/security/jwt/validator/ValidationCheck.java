package org.activiti.cloud.services.common.security.jwt.validator;

import org.springframework.security.oauth2.jwt.Jwt;

public interface ValidationCheck {

    boolean isValid(Jwt accessToken);

}
