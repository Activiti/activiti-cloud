package org.activiti.cloud.services.identity.keycloak;

import org.activiti.cloud.services.common.security.keycloak.config.CommonSecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication(exclude = CommonSecurityAutoConfiguration.class)
@EnableWebSecurity
public class KeycloakTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeycloakTestApplication.class, args);
    }

}
