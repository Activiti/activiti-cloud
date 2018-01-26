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

package org.activiti.cloud.connectors.starter.model;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.messaging.Message;

import static org.activiti.test.Assertions.assertThat;

public class IntegrationResultEventBuilderTest {

    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String EXEC_ID = "execId";
    private static final String FLOW_NODE_ID = "flowNodeId";
    private static final String APP_NAME = "appName";
    private static final String VAR = "var";
    private static final String VALUE = "value";

    @Test
    public void shouldBuildIntegrationResultBasedOnInformationFromIntegrationRequest() throws Exception {
        //given
        IntegrationRequestEvent integrationRequestEvent = new IntegrationRequestEvent(PROC_INST_ID,
                                                                                      PROC_DEF_ID,
                                                                                      EXEC_ID,
                                                                                      Collections.emptyMap());
        integrationRequestEvent.setFlowNodeId(FLOW_NODE_ID);
        integrationRequestEvent.setApplicationName(APP_NAME);

        //when
        IntegrationResultEvent resultEvent = IntegrationResultEventBuilder
                .resultFor(integrationRequestEvent)
                .withVariables(Collections.singletonMap(VAR,
                                                        VALUE))
                .build();

        //then
        assertThat(resultEvent)
                .hasExecutionId(EXEC_ID)
                .hasFlowNodeId(FLOW_NODE_ID)
                .hasTargetApplication(APP_NAME)
                .hasVariables(Collections.singletonMap(VAR,
                                                       VALUE));
    }

    @Test
    public void shouldBuildMessageWithTargetApplicationHeader() throws Exception {
        //given
        IntegrationRequestEvent integrationRequestEvent = new IntegrationRequestEvent(PROC_INST_ID,
                                                                                      PROC_DEF_ID,
                                                                                      EXEC_ID,
                                                                                      Collections.emptyMap());
        integrationRequestEvent.setApplicationName(APP_NAME);

        //when
        Message<IntegrationResultEvent> message = IntegrationResultEventBuilder
                .resultFor(integrationRequestEvent)
                .buildMessage();

        //then
        Assertions.assertThat(message.getHeaders()).containsEntry("targetApplication",
                                                                  APP_NAME);
    }
}