package org.activiti.cloud.starter.messages.test.redis;

import java.io.IOException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

public class RedisApplicationInitializer implements
    ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int CONTAINER_EXIT_CODE_OK = 0;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static GenericContainer container = new GenericContainer("redis")
        .withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        if (container.isRunning()) {
            return;
        }

        container.start();

        try {

            TestPropertyValues.of(
                "spring.redis.host=" + container.getContainerIpAddress(),
                "spring.redis.port=" + container.getFirstMappedPort()
            ).applyTo(context.getEnvironment());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
