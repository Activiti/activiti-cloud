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
        return accessToken.getNotBefore() == null || currentTime(offset) >= accessToken.getNotBefore().toEpochMilli();
    }
}
