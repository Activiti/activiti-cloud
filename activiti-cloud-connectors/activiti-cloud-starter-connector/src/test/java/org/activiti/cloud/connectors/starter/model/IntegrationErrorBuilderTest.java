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
package org.activiti.cloud.connectors.starter.model;

import static org.activiti.test.Assertions.assertThat;

import java.util.Collections;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.connectors.starter.configuration.ConnectorProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class IntegrationErrorBuilderTest {

    private static final String PROC_INST_ID = "procInstId";
    private static final String PROC_DEF_ID = "procDefId";
    private static final String ACTIVITY_ELEMENT_ID = "activitiElementId";
    private static final String RB_NAME = "rbName";
    private static final String APP_NAME = "appName";

    private ConnectorProperties connectorProperties = new ConnectorProperties();

    @Test
    public void shouldBuildIntegrationErrorBasedOnInformationFromIntegrationRequest() throws Exception {
        //given
        Throwable error = new Error("Boom!");

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setClientId(ACTIVITY_ELEMENT_ID);
        integrationContext.addInBoundVariables(Collections.emptyMap());
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);

        IntegrationRequestImpl integrationRequestEvent = new IntegrationRequestImpl(integrationContext);
        integrationRequestEvent.setAppName(APP_NAME);
        integrationRequestEvent.setServiceFullName(RB_NAME);

        //when
        IntegrationError integrationError = IntegrationErrorBuilder.errorFor(integrationRequestEvent,
                                                                             connectorProperties,
                                                                             error)
                                                                   .build();
        //then
        assertThat(integrationError)
                .hasIntegrationContext(integrationContext)
                .hasIntegrationRequest(integrationRequestEvent)
                .hasErrorClassName("java.lang.Error")
                .hasErrorMessage("Boom!")
                .hasStackTraceElements(error.getStackTrace());

        assertThat(integrationContext)
                .hasClientId(ACTIVITY_ELEMENT_ID);
    }

    @Test
    public void shouldBuildMessageWithRequiredHeader() throws Exception {
        //given
        Throwable error = new Error("Boom!");

        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setClientId(ACTIVITY_ELEMENT_ID);
        integrationContext.addInBoundVariables(Collections.emptyMap());
        integrationContext.setProcessDefinitionId(PROC_DEF_ID);
        integrationContext.setProcessInstanceId(PROC_INST_ID);

        IntegrationRequestImpl integrationRequestEvent = new IntegrationRequestImpl(integrationContext);
        integrationRequestEvent.setAppName(APP_NAME);
        integrationRequestEvent.setServiceFullName(RB_NAME);

        //when
        Message<IntegrationError> message = IntegrationErrorBuilder.errorFor(integrationRequestEvent,
                                                                             connectorProperties,
                                                                             error)
                                                                   .buildMessage();

        //then
        Assertions.assertThat(message.getHeaders())
                  .containsEntry(MessageHeaders.CONTENT_TYPE, "application/json")
                  .containsEntry("targetService", RB_NAME)
                  .containsEntry("targetAppName", APP_NAME);
    }
}
