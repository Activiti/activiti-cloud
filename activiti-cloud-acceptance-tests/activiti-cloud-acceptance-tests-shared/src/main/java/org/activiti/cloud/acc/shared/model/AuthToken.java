/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import tools.jackson.databind.ObjectMapper;

public class AuthToken {

    private String access_token;

    public String getAccess_token() {
        return access_token;
    }

    @Override
    public String toString() {
        return access_token;
    }

    public String getSubject() {
        return getClaim("sub");
    }

    public String getClaim(String claimName) {
        if (access_token == null) {
            return null;
        }

        try {
            String[] parts = access_token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> claims = mapper.readValue(payload, Map.class);

            Object claim = claims.get(claimName);
            if (claim instanceof Map) {
                return claim.toString();
            }
            return claim != null ? claim.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
