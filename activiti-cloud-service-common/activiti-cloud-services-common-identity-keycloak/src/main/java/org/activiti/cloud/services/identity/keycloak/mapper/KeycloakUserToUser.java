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
package org.activiti.cloud.services.identity.keycloak.mapper;

import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakUser;

public class KeycloakUserToUser {

    public static User toUser(KeycloakUser kUser) {
//        return User.builder()
//            .id(kUser.getId())
//            .firstName(kUser.getFirstName())
//            .lastName(kUser.getLastName())
//            .username(kUser.getUsername())
//            .email(kUser.getEmail())
//            .displayName(String.join(" ",
//                kUser.getFirstName(),
//                kUser.getLastName()))
//            .build();


        User user = new User();
        user.setId(kUser.getId());
        user.setUsername(kUser.getUsername());
        user.setDisplayName(String.join(" ", kUser.getFirstName(), kUser.getLastName()));
        user.setFirstName(kUser.getFirstName());
        user.setLastName(kUser.getLastName());
        user.setEmail(kUser.getEmail());
        return user;
    }

}
