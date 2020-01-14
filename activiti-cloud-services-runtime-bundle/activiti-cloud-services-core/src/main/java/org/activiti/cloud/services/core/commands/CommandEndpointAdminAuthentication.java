/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.core.commands;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class CommandEndpointAdminAuthentication extends UsernamePasswordAuthenticationToken {

    private static final long serialVersionUID = 1L;

    public CommandEndpointAdminAuthentication(Object principal,
                                              Object credentials,
                                              Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }

    public CommandEndpointAdminAuthentication() {
        this("commandExecutor",
             null,
             Arrays.asList(new SimpleGrantedAuthority("ROLE_ACTIVITI_ADMIN")));
    }

}
