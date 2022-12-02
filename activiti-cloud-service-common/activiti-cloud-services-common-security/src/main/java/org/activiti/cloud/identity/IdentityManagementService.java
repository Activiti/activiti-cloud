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
package org.activiti.cloud.identity;

import org.activiti.cloud.identity.model.Group;
import org.activiti.cloud.identity.model.SecurityRequestBodyRepresentation;
import org.activiti.cloud.identity.model.SecurityResponseRepresentation;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.identity.model.UserRoles;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;

/**
 *
 * Interface used to handle Identity business logic
 * <p>For general purpose you should use {@link org.activiti.cloud.identity.IdentityService}
 *
 */
public interface IdentityManagementService {

  List<User> findUsers(UserSearchParams userSearchParams);

  List<Group> findGroups(GroupSearchParams groupSearchParams);

  UserRoles getUserRoles(Jwt principal);

  void addApplicationPermissions(String application, List<SecurityRequestBodyRepresentation> securityRequestBodyRepresentations);

  List<SecurityResponseRepresentation> getApplicationPermissions (String application, Set<String> roles);

}
