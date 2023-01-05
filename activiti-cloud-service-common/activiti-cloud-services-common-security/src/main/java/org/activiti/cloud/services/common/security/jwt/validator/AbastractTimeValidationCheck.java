package org.activiti.cloud.services.common.security.jwt.validator;

public interface AbastractTimeValidationCheck extends ValidationCheck {

    default long currentTime(Long offset) {
        return System.currentTimeMillis() + offset;
    }

}
