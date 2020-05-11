package org.activiti.cloud.starter.audit.tests.it;

import java.util.stream.Stream;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

public class ContainersApplicationInitializer implements
    ApplicationContextInitializer<ConfigurableApplicationContext> {


    private static GenericContainer keycloakContainer = new GenericContainer(
        "activiti/activiti-keycloak")
        .withExposedPorts(8180)
        .waitingFor(Wait.defaultWaitStrategy());

    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
        "rabbitmq:management");

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        if (!keycloakContainer.isRunning() && !rabbitMQContainer.isRunning()) {
            Startables.deepStart(Stream.of(keycloakContainer, rabbitMQContainer)).join();
        }

        TestPropertyValues.of(
            "keycloak.auth-server-url=" + "http://" + keycloakContainer.getContainerIpAddress()
                + ":" + keycloakContainer.getFirstMappedPort() + "/auth",
            "spring.rabbitmq.host=" + rabbitMQContainer.getContainerIpAddress(),
            "spring.rabbitmq.port=" + String.valueOf(rabbitMQContainer.getAmqpPort())
        ).applyTo(context.getEnvironment());

    }
}
