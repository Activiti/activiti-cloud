package org.activiti.cloud.starter.tests.helper;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({ProcessInstanceRestTemplate.class,
        TaskRestTemplate.class,
        MessageRestTemplate.class,
        ProcessDefinitionRestTemplate.class,
        SignalRestTemplate.class})
public class HelperConfiguration {

}