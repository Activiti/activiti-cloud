package org.activiti.cloud.services.identity.keycloak;

import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class KeycloakAuthenticationProviderTest {

    private KeycloakActivitiAuthenticationProvider keycloakActivitiAuthenticationProvider = new KeycloakActivitiAuthenticationProvider();

    @Test
    public void testAuthenticate(){
        RefreshableKeycloakSecurityContext keycloakSecurityContext = new RefreshableKeycloakSecurityContext();
        KeycloakPrincipal principal = new KeycloakPrincipal("bob",keycloakSecurityContext);
        KeycloakAccount keycloakAccount = new SimpleKeycloakAccount(principal, new HashSet<>(Arrays.asList("role1","role2")),keycloakSecurityContext);

        KeycloakAuthenticationToken token = new KeycloakAuthenticationToken(keycloakAccount);

        assertThat(keycloakActivitiAuthenticationProvider.authenticate(token)).isNotNull();

    }
}
