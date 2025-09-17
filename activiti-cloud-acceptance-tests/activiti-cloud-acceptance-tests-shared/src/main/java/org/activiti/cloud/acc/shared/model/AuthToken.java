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
package org.activiti.cloud.acc.shared.model;

import java.util.Map;
import org.springframework.security.oauth2.jwt.JwtDecoder;

public class AuthToken {

    private final JwtDecoder jwtDecoder;
    private String access_token;

    public AuthToken(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public String getAccess_token() {
        return access_token;
    }

    @Override
    public String toString() {
        return access_token;
    }

    public String getClaim(String claimName) {
        if (access_token == null) {
            return null;
        }

        try {
            var jwt = jwtDecoder.decode(access_token);
            var claim = (Map<String, Object>) jwt.getClaim(claimName);
            return claim.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getSubject() {
        return getClaim("sub");
    }
}
