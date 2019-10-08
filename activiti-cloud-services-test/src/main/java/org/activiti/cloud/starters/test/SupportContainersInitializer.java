package org.activiti.cloud.starters.test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.util.stream.Stream;


public class SupportContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static GenericContainer keycloakContainer;

    private static RabbitMQContainer rabbitMQContainer;


    private static void startContainers() {
        if (keycloakContainer == null && rabbitMQContainer == null) {
            keycloakContainer = new GenericContainer("activiti/activiti-keycloak")
                    .withExposedPorts(8180)
                    .waitingFor(Wait.defaultWaitStrategy());


            rabbitMQContainer = new RabbitMQContainer("rabbitmq:management");
            Startables.deepStart(Stream.of(keycloakContainer, rabbitMQContainer)).join();
        }
    }

    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

        startContainers();

        TestPropertyValues.of(
                "keycloak.auth-server-url=http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getFirstMappedPort() + "/auth",
                "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress(),
                "spring.rabbitmq.port=" + rabbitMQContainer.getAmqpPort()
        ).applyTo(configurableApplicationContext.getEnvironment());
    }
}

