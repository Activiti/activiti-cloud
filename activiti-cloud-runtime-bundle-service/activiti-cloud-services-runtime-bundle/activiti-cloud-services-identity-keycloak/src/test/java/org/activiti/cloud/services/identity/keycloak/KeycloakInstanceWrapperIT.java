package org.activiti.cloud.services.identity.keycloak;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource("classpath:application.properties")
public class KeycloakInstanceWrapperIT {

    @Autowired
    private KeycloakInstanceWrapper keycloakInstanceWrapper;

    @Test
    public void shouldWireWrapper() {
        assertThat(keycloakInstanceWrapper).isNotNull();
        assertThat(keycloakInstanceWrapper.getRealm()).isNotNull();
    }
}
