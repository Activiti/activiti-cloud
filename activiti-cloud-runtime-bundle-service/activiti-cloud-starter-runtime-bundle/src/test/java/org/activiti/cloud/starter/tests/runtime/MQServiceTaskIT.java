/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.starter.tests.runtime;

import org.activiti.api.process.model.IntegrationContext;
import org.activiti.engine.integration.IntegrationContextService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.services.connectors.channel.IntegrationRequestReplayer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.config.BindingProperties;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MQServiceTaskIT extends AbstractMQServiceTaskIT {

    @Autowired
    private CanFailConnector canFailConnector;

    @Autowired
    private IntegrationRequestReplayer integrationRequestReplayer;

    @Autowired
    private IntegrationContextService integrationContextService;

    @Test
    public void shouldConfigureDefaultConnectorBindingProperties() {
        //given

        //when
        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();

        //then
        assertThat(bindings)
            .extractingFromEntries(entry -> new AbstractMap.SimpleEntry<String, String>(entry.getKey(),
                                                                                        entry.getValue()
                                                                                             .getDestination()))
            .contains(entry("mealsConnector", "mealsConnector"),
                      entry("rest.GET", "rest.GET"),
                      entry("perfromBusinessTask", "perfromBusinessTask"),
                      entry("anyImplWithoutHandler", "anyImplWithoutHandler"),
                      entry("payment", "payment"),
                      entry("Constants Connector.constantsActionName", "Constants Connector.constantsActionName"),
                      entry("Variable Mapping Connector.variableMappingActionName", "Variable Mapping Connector.variableMappingActionName"),
                      entry("miCloudConnector", "miCloudConnector"));
    }

    @Test
    public void shouldRecoverFromFailure() {
        //given
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", "John");
        ProcessInstance procInst = runtimeService.startProcessInstanceByKey("MQServiceTaskErrorRecoverProcess",
                                                                            "businessKey",
                                                                            variables);
        assertThat(procInst).isNotNull();
        await("the service task should fail the execution")
            .untilTrue(canFailConnector.errorSent());

        assertThat(taskService.createTaskQuery()
                              .processInstanceId(procInst.getProcessInstanceId())
                              .list()).isEmpty();
        //when
        IntegrationContext integrationContext = canFailConnector.getLatestReceivedIntegrationRequest()
                                                                .getIntegrationContext();
        canFailConnector.setShouldSendError(false);
        integrationRequestReplayer.replay(integrationContext.getExecutionId(),
                                          integrationContext.getClientId());

        //then
        await("the execution should arrive in the human tasks which follows the service task")
            .untilAsserted(() -> {
                    List<Task> tasks = taskService.createTaskQuery().processInstanceId(procInst.getProcessInstanceId()).list();
                    assertThat(tasks).isNotNull();
                    assertThat(tasks).extracting(Task::getName).containsExactly("Schedule meeting after service");
                }
            );
    }

}
