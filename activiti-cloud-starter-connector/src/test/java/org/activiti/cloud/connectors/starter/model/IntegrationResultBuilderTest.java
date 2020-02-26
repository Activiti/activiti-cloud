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

import static org.activiti.test.Assertions.assertThat;

import java.util.Collections;

import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;

public class IntegrationResultBuilderTest {

    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String ACTIVITY_ELEMENT_ID = "activitiElementId";
    private static final String RB_NAME = "appName";
    private static final String VAR = "var";
    private static final String VALUE = "value";

    @Autowired
    private ConnectorProperties connectorProperties;

    @Test
    public void shouldBuildIntegrationResultBasedOnInformationFromIntegrationRequest() throws Exception {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setClientId(ACTIVITY_ELEMENT_ID);
        integrationContext.setInBoundVariables(Collections.emptyMap());
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);

        IntegrationRequestImpl integrationRequestEvent = new IntegrationRequestImpl(integrationContext);
        integrationRequestEvent.setAppName(RB_NAME);

        //when
        IntegrationResult resultEvent = IntegrationResultBuilder
                .resultFor(integrationRequestEvent,
                           connectorProperties)
                .withOutboundVariables(Collections.singletonMap(VAR,
                                                                VALUE))
                .build();

        //then
        assertThat(resultEvent)
                .hasIntegrationContext(integrationContext)
                .hasIntegrationRequest(integrationRequestEvent);
        assertThat(integrationContext)
                .hasClientId(ACTIVITY_ELEMENT_ID)
                .hasOutBoundVariables(Collections.singletonMap(VAR,
                                                               VALUE));
    }

    @Test
    public void shouldBuildMessageWithTargetApplicationHeader() throws Exception {
        //given
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setClientId(ACTIVITY_ELEMENT_ID);
        integrationContext.setInBoundVariables(Collections.emptyMap());
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);

        IntegrationRequestImpl integrationRequestEvent = new IntegrationRequestImpl(integrationContext);
        integrationRequestEvent.setServiceFullName(RB_NAME);
        //when
        Message<IntegrationResult> message = IntegrationResultBuilder
                .resultFor(integrationRequestEvent, connectorProperties)
                .buildMessage();

        //then
        Assertions.assertThat(message.getHeaders()).containsEntry("targetService",
                                                                  RB_NAME);
    }
}