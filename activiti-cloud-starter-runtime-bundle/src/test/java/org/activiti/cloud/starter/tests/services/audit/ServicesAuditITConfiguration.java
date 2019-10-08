package org.activiti.cloud.starter.tests.services.audit;

import org.activiti.cloud.starter.tests.helper.HelperConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({HelperConfiguration.class,
        AuditConsumerStreamHandler.class})
public class ServicesAuditITConfiguration {

}
