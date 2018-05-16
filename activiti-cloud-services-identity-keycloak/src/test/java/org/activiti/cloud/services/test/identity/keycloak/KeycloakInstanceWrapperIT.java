package org.activiti.cloud.services.test.identity.keycloak;

import org.activiti.cloud.services.identity.keycloak.KeycloakInstanceWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource("classpath:application.properties")
public class KeycloakInstanceWrapperIT {

    @Autowired
    private KeycloakInstanceWrapper keycloakInstanceWrapper;

    @Test
    public void shouldWireWrapper(){
        assertThat(keycloakInstanceWrapper).isNotNull();
        assertThat(keycloakInstanceWrapper.getRealm()).isNotNull();
    }
}
