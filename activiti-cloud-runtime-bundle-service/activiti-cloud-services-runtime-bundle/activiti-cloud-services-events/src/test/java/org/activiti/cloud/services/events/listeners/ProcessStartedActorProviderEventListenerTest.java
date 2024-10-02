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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.identity.IdentityService;
import org.activiti.cloud.identity.model.User;
import org.activiti.cloud.services.events.ActorConstants;
import org.activiti.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessStartedActorProviderEventListenerTest {

    private static final String USERNAME_GUID = "964b5dff-173a-4ba2-947d-1db16c1236a7";

    private static final String USER_ID = "123b5dff-12ra-4ba2-947d-1db16c1236t9";

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private SecurityContextPrincipalProvider securityContextPrincipalProvider;

    @Mock
    private PrincipalIdentityProvider principalIdentityProvider;

    @Mock
    private Principal principal;

    @Mock
    private IdentityService identityService;

    private ProcessStartedActorProviderEventListener processStartedActorProviderEventListener;

    @BeforeEach
    void beforeEach() {
        User user = new User();
        user.setId(USER_ID);
        when(this.identityService.findUserByName(anyString())).thenReturn(user);
        when(this.securityContextPrincipalProvider.getCurrentPrincipal()).thenReturn(Optional.of(this.principal));
        this.processStartedActorProviderEventListener =
            new ProcessStartedActorProviderEventListener(
                this.runtimeService,
                this.securityContextPrincipalProvider,
                this.principalIdentityProvider,
                this.identityService
            );
    }

    @Test
    void should_setActorFromPrincipal_when_invokeProcessStartedActorProviderEventListenerOnEvent() {
        when(this.principal.getName()).thenReturn(USERNAME_GUID);
        CloudProcessCreatedEventImpl cloudProcessCreatedEvent = buildCloudProcessCreatedEvent();

        this.processStartedActorProviderEventListener.onEvent(cloudProcessCreatedEvent);

        verify(this.runtimeService)
            .addUserIdentityLink(
                cloudProcessCreatedEvent.getProcessInstanceId(),
                USER_ID,
                ActorConstants.ACTOR_TYPE,
                USERNAME_GUID.getBytes()
            );
    }

    @NotNull
    private CloudProcessCreatedEventImpl buildCloudProcessCreatedEvent() {
        ProcessInstanceImpl process = new ProcessInstanceImpl();
        process.setId(UUID.randomUUID().toString());
        process.setInitiator("initiator");
        CloudProcessCreatedEventImpl cloudProcessCreatedEvent = new CloudProcessCreatedEventImpl(process);
        return cloudProcessCreatedEvent;
    }
}
