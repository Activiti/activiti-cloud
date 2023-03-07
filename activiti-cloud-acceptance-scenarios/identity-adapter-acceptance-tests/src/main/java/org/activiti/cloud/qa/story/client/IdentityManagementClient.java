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
package org.activiti.cloud.qa.story.client;

import feign.Headers;
import java.util.List;
import java.util.Set;
import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.identity.model.UserRoles;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

public interface IdentityManagementClient {
    @RequestMapping(method = RequestMethod.GET, value = "/roles")
    @Headers("Content-Type: application/json")
    UserRoles getUserRoles();

    @RequestMapping(method = RequestMethod.GET, value = "/groups")
    @Headers("Content-Type: application/json")
    List<Group> searchGroups(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "role", required = false) Set<String> roles,
        @RequestParam(value = "application", required = false) String application
    );

    @RequestMapping(method = RequestMethod.GET, value = "/users")
    @Headers("Content-Type: application/json")
    List<User> searchUsers(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "role", required = false) Set<String> roles,
        @RequestParam(value = "group", required = false) Set<String> groups,
        @RequestParam(value = "application", required = false) String application
    );
}
