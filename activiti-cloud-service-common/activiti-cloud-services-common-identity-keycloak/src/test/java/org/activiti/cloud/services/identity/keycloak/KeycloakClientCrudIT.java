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
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = {KeycloakClientApplication.class},
    properties = {
        "keycloak.realm=activiti",
        "keycloak.use-resource-role-mappings=false",
        "identity.client.cache.cacheExpireAfterWrite=PT5s",
        "keycloak.user=admin"}
)
@ContextConfiguration(initializers = {KeycloakContainerApplicationInitializer.class})
public class KeycloakClientCrudIT {

    @Autowired
    private KeycloakClient keycloakClient;

    @Test
    public void should_handleClientCRUD() {
        String clientId = "crudClientId";
        KeycloakClientRepresentation client = KeycloakClientRepresentation.Builder.newKeycloakClientRepresentationBuilder()
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
        assertThat(HttpStatus.valueOf(response.status()).is2xxSuccessful()).isTrue();

        String idOfClient = getIdOfClient(client.getClientId());
        KeycloakClientRepresentation clientCreated = keycloakClient.getClientById(idOfClient);
        assertThat(clientCreated).isNotNull();
        assertThat(clientCreated.getClientId()).isEqualTo(clientId);

        clientCreated.setDirectAccessGrantsEnabled(false);
        keycloakClient.updateClient(idOfClient, clientCreated);
        KeycloakClientRepresentation clientUpdated = keycloakClient.getClientById(idOfClient);
        assertThat(clientUpdated.getDirectAccessGrantsEnabled()).isFalse();

        keycloakClient.deleteClient(idOfClient);
        Throwable exception = catchThrowable(() -> keycloakClient.getClientById(idOfClient));
        assertThat(exception)
            .isInstanceOf(FeignException.NotFound.class);
    }


    private String getIdOfClient(String clientId) {
        List<KeycloakClientRepresentation> clients = keycloakClient.findByClientId(clientId);
        return clients.get(0).getId();
    }

}
