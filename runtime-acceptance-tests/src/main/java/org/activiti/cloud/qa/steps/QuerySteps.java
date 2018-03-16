/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.steps;

import java.util.Map;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.config.RuntimeTestsConfigurationProperties;
import org.activiti.cloud.qa.model.ProcessInstance;
import org.activiti.cloud.qa.model.QueryStatus;
import org.activiti.cloud.qa.rest.RuntimeFeignConfiguration;
import org.activiti.cloud.qa.rest.feign.EnableFeignContext;
import org.activiti.cloud.qa.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.*;

/**
 * Query steps
 */
@EnableFeignContext
@ContextConfiguration(classes = {RuntimeTestsConfigurationProperties.class, RuntimeFeignConfiguration.class})
public class QuerySteps {

    @Autowired
    private QueryService queryService;

    @Step
    public Map<String, Object> health() {
        return queryService.health();
    }

    @Step
    public ProcessInstance getProcessInstance(String processInstanceId) throws Exception {
        return queryService.getProcessInstance(processInstanceId);
    }

    @Step
    public void checkProcessInstanceStatus(String processInstanceId,
                                           QueryStatus expectedStatus) throws Exception {
        assertThat(expectedStatus).isNotNull();

        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getStatus()).isEqualTo(expectedStatus);
    }
}
