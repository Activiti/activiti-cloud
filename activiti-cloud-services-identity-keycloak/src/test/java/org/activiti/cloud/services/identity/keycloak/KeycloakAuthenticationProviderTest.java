package org.activiti.cloud.services.identity.keycloak;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeycloakAuthenticationProviderTest {

    private KeycloakActivitiAuthenticationProvider keycloakActivitiAuthenticationProvider = spy(new KeycloakActivitiAuthenticationProvider());
    private KeycloakAuthenticationToken token;
    private RefreshableKeycloakSecurityContext keycloakSecurityContext;

    @Before
    public void setUp(){
        keycloakSecurityContext = mock(RefreshableKeycloakSecurityContext.class);
        KeycloakPrincipal principal = new KeycloakPrincipal("bob",keycloakSecurityContext);
        KeycloakAccount keycloakAccount = new SimpleKeycloakAccount(principal, new HashSet<>(Arrays.asList("role1","role2")),keycloakSecurityContext);
        token = new KeycloakAuthenticationToken(keycloakAccount,false);

    }

    @Test
    public void authenticateShouldUseNameFromAuthenticationWhenPreferredUserNameIsNotSet(){

        //when
        Authentication authentication = keycloakActivitiAuthenticationProvider.authenticate(token);

        //then
        assertThat(authentication).isNotNull();
        verify(keycloakActivitiAuthenticationProvider).setAuthenticatedUserId("bob");

    }

    @Test
    public void authenticateShouldUsePreferredUsernameWhenSet(){

        //given
        AccessToken accessToken = mock(AccessToken.class);
        when(keycloakSecurityContext.getToken()).thenReturn(accessToken);
        when(accessToken.getPreferredUsername()).thenReturn("bob@any.org");

        //when
        Authentication authentication = keycloakActivitiAuthenticationProvider.authenticate(token);

        //then
        assertThat(authentication).isNotNull();
        verify(keycloakActivitiAuthenticationProvider).setAuthenticatedUserId("bob@any.org");

    }
}
