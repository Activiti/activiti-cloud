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
package org.activiti.cloud.services.common.security.keycloak;

import java.util.Optional;
import org.activiti.cloud.services.common.security.keycloak.config.JwtAdapter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAccessTokenValidator {

    private final long offset;

    public JwtAccessTokenValidator(long offset) {
        this.offset = offset;
    }

    public boolean isValid(@NonNull JwtAdapter jwtAdapter) {
        return Optional.ofNullable(jwtAdapter)
            .map(JwtAdapter::getJwt)
            .map(this::isActive)
            .orElseThrow(() -> new SecurityException("Invalid access token instance"));
    }

    private boolean isActive(Jwt accessToken) {
        return !isExpired(accessToken) && isNotBefore(accessToken);
    }

    /**
     * The 'nbf' (NotBefore) claim is used by some auth providers as
     * a way to set the moment from which a token starts being valid.
     * A token could not be expired, but if its validity period has not started,
     * it would be still an invalid token
     * @param accessToken the Jwt access token
     * @return if the nbf claim is either in the past or the future
     */
    private boolean isNotBefore(Jwt accessToken) {
        return accessToken.getNotBefore() == null ||
            currentTime() >= accessToken.getNotBefore().toEpochMilli();
    }

    private boolean isExpired(Jwt accessToken) {
        return accessToken.getExpiresAt() != null &&
            accessToken.getExpiresAt().toEpochMilli() != 0 &&
            currentTime() > accessToken.getExpiresAt().toEpochMilli();
    }

    private long currentTime() {
        return System.currentTimeMillis() + offset;
    }

}
