package org.activiti.cloud.starter.tests.runtime;

import org.activiti.cloud.starter.tests.helper.HelperConfiguration;
import org.activiti.cloud.starter.tests.util.VariablesUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({HelperConfiguration.class,
        ServiceTaskConsumerHandler.class,
        VariablesUtil.class})
public class RuntimeITConfiguration {

}
