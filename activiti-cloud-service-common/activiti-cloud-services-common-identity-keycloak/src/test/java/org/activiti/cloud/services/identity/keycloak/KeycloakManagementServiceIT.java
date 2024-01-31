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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.activiti.cloud.identity.UserSearchParams;
import org.activiti.cloud.identity.UserTypeSearchParam;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = { KeycloakClientApplication.class },
    properties = {
        "keycloak.realm=activiti",
        "keycloak.use-resource-role-mappings=false",
        "identity.client.cache.cacheExpireAfterWrite=PT5s",
    }
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
class KeycloakManagementServiceIT {

    @Autowired
    private KeycloakManagementService keycloakManagementService;

    @Test
    void should_Not_RetrieveServiceAccounts_WhenUserTypeSearchParamIsInteractive() {
        UserSearchParams searchParams = new UserSearchParams();
        searchParams.setSearch("");
        searchParams.setType(UserTypeSearchParam.INTERACTIVE);

        List<User> users = keycloakManagementService.findUsers(searchParams);

        assertThat(users).isNotEmpty();
        assertThat(users).noneMatch(user -> user.getUsername().startsWith("service-account"));
    }

    @Test
    void should_Not_RetrieveServiceAccounts_WhenUserTypeSearchParamIsNull() {
        UserSearchParams searchParams = new UserSearchParams();
        searchParams.setSearch("");

        assertThat(searchParams.getType()).isNull();

        List<User> users = keycloakManagementService.findUsers(searchParams);

        assertThat(users).isNotEmpty();
        assertThat(users).noneMatch(user -> user.getUsername().startsWith("service-account"));
    }

    @Test
    void should_RetrieveUsersAndServiceAccounts_WhenUserTypeSearchParamIsAll() {
        UserSearchParams searchParams = new UserSearchParams();
        searchParams.setSearch("");
        searchParams.setType(UserTypeSearchParam.INTERACTIVE);

        List<User> justUsers = keycloakManagementService.findUsers(searchParams);

        searchParams.setType(UserTypeSearchParam.ALL);
        List<User> usersAndServiceAccounts = keycloakManagementService.findUsers(searchParams);

        assertThat(usersAndServiceAccounts.size()).isGreaterThan(justUsers.size());
        assertThat(usersAndServiceAccounts)
            .allMatch(element -> element.getUsername().startsWith("service-account") ^ justUsers.contains(element));
    }
}
