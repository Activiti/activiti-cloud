package org.activiti.cloud.starter.messages.test.redis;

import java.io.IOException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

public class RedisApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {


    private static GenericContainer container = new GenericContainer("redis")
        .withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        container.start();

        TestPropertyValues.of(
            "spring.redis.host=" + container.getContainerIpAddress(),
            "spring.redis.port=" + container.getFirstMappedPort()
        ).applyTo(context.getEnvironment());

    }


}
