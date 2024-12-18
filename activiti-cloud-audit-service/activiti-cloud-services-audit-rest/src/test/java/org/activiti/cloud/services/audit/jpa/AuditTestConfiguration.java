package org.activiti.cloud.services.audit.jpa;

import org.activiti.cloud.services.audit.jpa.util.TestConverter;
import org.springframework.context.annotation.Bean;

public class AuditTestConfiguration {

    @Bean
    public TestConverter getTestConverter() {
        return new TestConverter();
    }
}
