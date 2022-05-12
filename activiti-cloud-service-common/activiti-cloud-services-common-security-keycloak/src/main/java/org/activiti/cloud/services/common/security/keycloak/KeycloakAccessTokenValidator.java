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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Optional;
import org.activiti.cloud.services.common.security.keycloak.config.JwtAdapter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakAccessTokenValidator {

    private long offset = 0;

    public void setOffset(int offset) {
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

    private boolean isNotBefore(Jwt accessToken) {
        return accessToken.getNotBefore() == null ||
            currentTime() >= accessToken.getNotBefore().toEpochMilli();
    }

    private boolean isExpired(Jwt accessToken) {
        return accessToken.getExpiresAt() != null &&
            accessToken.getExpiresAt().toEpochMilli() != 0 &&
            currentTime() > accessToken.getExpiresAt().toEpochMilli();
    }

    public long currentTime() {
        return System.currentTimeMillis() + offset;
    }

}
