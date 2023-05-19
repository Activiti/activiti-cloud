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

package org.activiti.cloud.services.identity.keycloak;

import feign.Headers;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public interface TestKeycloakClient extends KeycloakClient {
    @RequestMapping(method = RequestMethod.POST, value = "/users")
    @Headers("Content-Type: application/json")
    void addUser(TestKeycloakUser user);

    @RequestMapping(method = RequestMethod.DELETE, value = "/users/{id}")
    @Headers("Content-Type: application/json")
    void deleteUser(@PathVariable("id") String id);
}
