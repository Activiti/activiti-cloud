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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import java.util.Map;
import org.springframework.graphql.server.support.AuthenticationExtractor;
import org.springframework.graphql.server.support.BearerTokenAuthenticationExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import reactor.core.publisher.Mono;

public class JWSBearerTokenAuthenticationExtractor implements AuthenticationExtractor {

    private final BearerTokenAuthenticationExtractor bearerTokenAuthenticationExtractor;

    public JWSBearerTokenAuthenticationExtractor(
        BearerTokenAuthenticationExtractor bearerTokenAuthenticationExtractor
    ) {
        this.bearerTokenAuthenticationExtractor = bearerTokenAuthenticationExtractor;
    }

    @Override
    public Mono<Authentication> getAuthentication(Map<String, Object> payload) {
        return bearerTokenAuthenticationExtractor
            .getAuthentication(payload)
            .filter(auth -> auth instanceof BearerTokenAuthenticationToken)
            .map(auth -> new JWSAuthentication((BearerTokenAuthenticationToken) auth));
    }
}
