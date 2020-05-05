package org.activiti.cloud.starter.messages.test.jdbc;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private PostgreSQLContainer container = new PostgreSQLContainer("postgres:10");

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        container.start();

        TestPropertyValues.of("spring.datasource.url=" + container.getJdbcUrl(),
            "spring.datasource.username=" + container.getUsername(),
            "spring.datasource.password=" + container.getPassword()
        ).applyTo(context.getEnvironment());

    }
}
