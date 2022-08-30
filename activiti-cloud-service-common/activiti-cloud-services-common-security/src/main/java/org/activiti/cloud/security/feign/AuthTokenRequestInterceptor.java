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
package org.activiti.cloud.security.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Optional;

public interface AuthTokenRequestInterceptor extends RequestInterceptor {

    String AUTHORIZATION = "Authorization";
    String BEARER = "Bearer";

    Optional<String> getToken();

    @Override
    default void apply(RequestTemplate template) {
        getToken()
            .ifPresent(token -> {
                template.removeHeader(AUTHORIZATION);
                template.header(AUTHORIZATION,
                                String.format("%s %s",
                                              BEARER,
                                              token));
            });
    }

}
