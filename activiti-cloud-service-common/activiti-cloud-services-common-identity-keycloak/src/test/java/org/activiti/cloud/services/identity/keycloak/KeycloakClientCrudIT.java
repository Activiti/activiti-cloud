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
import static org.assertj.core.api.Assertions.catchThrowable;

import feign.FeignException;
import feign.Response;
import java.util.Collections;
import java.util.List;
import org.activiti.cloud.services.identity.keycloak.client.KeycloakClient;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakClientRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakCredentialRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakCredentialRequestRepresentation;
import org.activiti.cloud.services.identity.keycloak.model.KeycloakRoleMapping;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
public class KeycloakClientCrudIT {

    public static final String ADMIN_USER_ID = "5f682999-d11d-4a42-bc42-86b7e6752223";

    @Autowired
    private KeycloakClient keycloakClient;

    @Test
    public void should_handleClientCRUD() {
        String clientId = "crudClientId";
        KeycloakClientRepresentation client = KeycloakClientRepresentation.Builder
            .newKeycloakClientRepresentationBuilder()
            .withClientId(clientId)
            .withClientName(clientId)
            .enabled(true)
            .directAccessGrantsEnabled(true)
            .withRedirectUris(Collections.emptyList())
            .withWebOrigins(Collections.emptyList())
            .publicClient(true)
            .implicitFlowEnabled(true)
            .withAccessTokenLifespanInSeconds(120)
            .build();
        Response response = keycloakClient.createClient(client);
        assertThat(HttpStatus.valueOf(response.status()).is2xxSuccessful()).isTrue();

        String idOfClient = getIdOfClient(client.getClientId());
        KeycloakClientRepresentation clientCreated = keycloakClient.getClientById(idOfClient);
        assertThat(clientCreated).isNotNull();
        assertThat(clientCreated.getClientId()).isEqualTo(clientId);
        assertThat(clientCreated.getAttributes().accessTokenLifespan()).isEqualTo(120);

        clientCreated.setDirectAccessGrantsEnabled(false);
        clientCreated.setAttributes(new KeycloakClientRepresentation.ClientAttributes(null));
        keycloakClient.updateClient(idOfClient, clientCreated);
        KeycloakClientRepresentation clientUpdated = keycloakClient.getClientById(idOfClient);
        assertThat(clientUpdated.getDirectAccessGrantsEnabled()).isFalse();
        assertThat(clientUpdated.getAttributes().accessTokenLifespan()).isNull();

        keycloakClient.deleteClient(idOfClient);
        Throwable exception = catchThrowable(() -> keycloakClient.getClientById(idOfClient));
        assertThat(exception).isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    public void should_createRoleRepresentationForClient() {
        String clientId = "crudClientId2";
        KeycloakClientRepresentation client = KeycloakClientRepresentation.Builder
            .newKeycloakClientRepresentationBuilder()
            .withClientId(clientId)
            .withClientName(clientId)
            .enabled(true)
            .directAccessGrantsEnabled(true)
            .withRedirectUris(Collections.emptyList())
            .withWebOrigins(Collections.emptyList())
            .publicClient(true)
            .implicitFlowEnabled(true)
            .build();
        Response response = keycloakClient.createClient(client);
        String idOfClient = getIdOfClient(client.getClientId());
        assertThat(HttpStatus.valueOf(response.status()).is2xxSuccessful()).isTrue();

        List<KeycloakRoleMapping> roleMappings = keycloakClient.getUserRoleMappingAvailable(ADMIN_USER_ID);
        KeycloakRoleMapping keycloakRoleMapping = roleMappings.get(0);
        keycloakClient.createRoleRepresentationForClient(idOfClient, keycloakRoleMapping);

        KeycloakRoleMapping roleRepresentationForClient = keycloakClient.getRoleRepresentationForClient(
            idOfClient,
            keycloakRoleMapping.getName()
        );
        assertThat(roleRepresentationForClient).isNotNull();
    }

    @Test
    public void should_handleClientWithServiceAccountEnabledCRUD() {
        String clientId = "crudClientWithServiceAccountId";
        KeycloakClientRepresentation client = KeycloakClientRepresentation.Builder
            .newKeycloakClientRepresentationBuilder()
            .withClientId(clientId)
            .withClientName(clientId)
            .enabled(true)
            .directAccessGrantsEnabled(true)
            .withRedirectUris(Collections.emptyList())
            .withWebOrigins(Collections.emptyList())
            .publicClient(true)
            .implicitFlowEnabled(true)
            .withAccessTokenLifespanInSeconds(120)
            .build();
        Response response = keycloakClient.createClient(client);
        assertThat(HttpStatus.valueOf(response.status()).is2xxSuccessful()).isTrue();

        String idOfClient = getIdOfClient(client.getClientId());
        KeycloakClientRepresentation clientCreated = keycloakClient.getClientById(idOfClient);
        assertThat(clientCreated).isNotNull();
        assertThat(clientCreated.getClientId()).isEqualTo(clientId);
        assertThat(clientCreated.getAttributes().accessTokenLifespan()).isEqualTo(120);

        clientCreated.setDirectAccessGrantsEnabled(false);
        clientCreated.setServiceAccountsEnabled(true);
        clientCreated.setPublicClient(false);
        clientCreated.setAttributes(new KeycloakClientRepresentation.ClientAttributes(null));
        keycloakClient.updateClient(idOfClient, clientCreated);
        KeycloakClientRepresentation clientUpdated = keycloakClient.getClientById(idOfClient);
        assertThat(clientUpdated.getDirectAccessGrantsEnabled()).isFalse();
        assertThat(clientUpdated.getServiceAccountsEnabled()).isTrue();
        assertThat(clientUpdated.getPublicClient()).isFalse();
        assertThat(clientUpdated.getAttributes().accessTokenLifespan()).isNull();

        KeycloakCredentialRequestRepresentation requestRepresentation = new KeycloakCredentialRequestRepresentation.Builder()
            .withId(idOfClient)
            .withRealm("activiti")
            .build();

        KeycloakCredentialRepresentation clientSecret = keycloakClient.createClientSecretById(
            requestRepresentation,
            idOfClient
        );
        assertThat(clientSecret).isNotNull();
        assertThat(clientSecret.getType()).isEqualTo("secret");
        assertThat(clientSecret.getValue()).isNotNull();
        assertThat(clientSecret.getValue()).isNotBlank();

        clientSecret = keycloakClient.getClientSecretById(idOfClient);
        assertThat(clientSecret).isNotNull();
        assertThat(clientSecret.getType()).isEqualTo("secret");
        assertThat(clientSecret.getValue()).isNotNull();
        assertThat(clientSecret.getValue()).isNotBlank();

        keycloakClient.deleteClient(idOfClient);
        Throwable exception = catchThrowable(() -> keycloakClient.getClientById(idOfClient));
        assertThat(exception).isInstanceOf(FeignException.NotFound.class);
    }

    private String getIdOfClient(String clientId) {
        List<KeycloakClientRepresentation> clients = keycloakClient.findByClientId(clientId);
        return clients.get(0).getId();
    }
}
