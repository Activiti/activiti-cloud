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

package org.activiti.cloud.services.events.listeners;

import java.util.Optional;
import org.activiti.api.process.runtime.events.ProcessCreatedEvent;
import org.activiti.api.process.runtime.events.listener.ProcessEventListener;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.cloud.identity.IdentityService;
import org.activiti.cloud.services.events.ActorConstants;
import org.activiti.engine.RuntimeService;

public class ProcessStartedActorProviderEventListener implements ProcessEventListener<ProcessCreatedEvent> {

    private final RuntimeService runtimeService;
    private final SecurityContextPrincipalProvider securityContextPrincipalProvider;
    private final PrincipalIdentityProvider principalIdentityProvider;
    private final IdentityService identityService;

    public ProcessStartedActorProviderEventListener(
        RuntimeService runtimeService,
        SecurityContextPrincipalProvider securityContextPrincipalProvider,
        PrincipalIdentityProvider principalIdentityProvider,
        IdentityService identityService
    ) {
        this.runtimeService = runtimeService;
        this.securityContextPrincipalProvider = securityContextPrincipalProvider;
        this.principalIdentityProvider = principalIdentityProvider;
        this.identityService = identityService;
    }

    @Override
    public void onEvent(ProcessCreatedEvent event) {
        securityContextPrincipalProvider
            .getCurrentPrincipal()
            .ifPresent(principal ->
                Optional
                    .ofNullable(principal.getName())
                    .map(String::getBytes)
                    .ifPresent(details ->
                        runtimeService.addUserIdentityLink(
                            event.getEntity().getId(),
                            identityService.findUserByName(event.getEntity().getInitiator()).getId(),
                            ActorConstants.ACTOR_TYPE,
                            details
                        )
                    )
            );
    }
}
