package org.activiti.cloud.services.events.listeners;

import java.util.Optional;
import org.activiti.api.process.runtime.events.ProcessStartedEvent;
import org.activiti.api.process.runtime.events.listener.ProcessRuntimeEventListener;
import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.engine.RuntimeService;

public class ProcessStartedEventIdentityLinkActorProviderEventListener
    implements ProcessRuntimeEventListener<ProcessStartedEvent> {

    private final RuntimeService runtimeService;
    private final SecurityContextPrincipalProvider securityContextPrincipalProvider;
    private final PrincipalIdentityProvider principalIdentityProvider;

    public ProcessStartedEventIdentityLinkActorProviderEventListener(
        RuntimeService runtimeService,
        SecurityContextPrincipalProvider securityContextPrincipalProvider,
        PrincipalIdentityProvider principalIdentityProvider
    ) {
        this.runtimeService = runtimeService;
        this.securityContextPrincipalProvider = securityContextPrincipalProvider;
        this.principalIdentityProvider = principalIdentityProvider;
    }

    @Override
    public void onEvent(ProcessStartedEvent event) {
        securityContextPrincipalProvider
            .getCurrentPrincipal()
            .ifPresent(principal ->
                Optional
                    .ofNullable(principal.getName())
                    .map(String::getBytes)
                    .ifPresent(details ->
                        runtimeService.addUserIdentityLink(
                            event.getEntity().getId(),
                            principalIdentityProvider.getUserId(principal),
                            "actor",
                            details
                        )
                    )
            );
    }
}
