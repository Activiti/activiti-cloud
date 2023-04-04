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
package org.activiti.cloud.services.common.security.jwt.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

public class ExpiredValidationCheck implements AbastractTimeValidationCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredValidationCheck.class);

    private final long offset;

    public ExpiredValidationCheck(long offset) {
        this.offset = offset;
    }

    @Override
    public boolean isValid(Jwt accessToken) {
        long currentTime = currentTime(offset);
        boolean result = !(
            accessToken.getExpiresAt() != null &&
                accessToken.getExpiresAt().toEpochMilli() != 0 &&
                currentTime > accessToken.getExpiresAt().toEpochMilli()
        );
        if(!result) {
            LOGGER.error("Current time {} is greater than expiration time {}", currentTime, accessToken.getExpiresAt());
        }
        return result;
    }
}
