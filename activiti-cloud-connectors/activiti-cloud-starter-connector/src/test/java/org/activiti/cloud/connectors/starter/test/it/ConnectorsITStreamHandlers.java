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
package org.activiti.cloud.connectors.starter.test.it;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.activiti.cloud.api.process.model.IntegrationError;
import org.activiti.cloud.api.process.model.IntegrationResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(ConnectorsITStreamHandlers.CONNECTOR_IT)
@Component
//@EnableBinding({RuntimeMockStreams.class, MockCloudRuntimeEventsChannels.class})
public class ConnectorsITStreamHandlers {

    public static final String CONNECTOR_IT = "ConnectorIT";

    private static AtomicInteger integrationResultEventsCounter = new AtomicInteger();
    private static AtomicBoolean integrationErrorEventProduced = new AtomicBoolean();
    private static AtomicReference<IntegrationError> integrationErrorReference = new AtomicReference<>();

    private String integrationId;

    public void consumeIntegrationResultsMock(IntegrationResult integrationResult) {
        assertThat(integrationResult.getIntegrationContext().getOutBoundVariables().get("var2")).isEqualTo(2L);
        assertThat(integrationResult.getIntegrationContext().getId()).isEqualTo(getIntegrationId());
        integrationResultEventsCounter.incrementAndGet();
    }

    public void consumeIntegrationErrorMock(IntegrationError integrationError) {
        integrationErrorReference.set(integrationError);

        integrationErrorEventProduced.set(true);
    }

    public AtomicInteger getIntegrationResultEventsCounter() {
        return integrationResultEventsCounter;
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }

    public AtomicBoolean isIntegrationErrorEventProduced() {
        return integrationErrorEventProduced;
    }

    public IntegrationError getIntegrationError() {
        return integrationErrorReference.get();
    }

    public void reset() {
        integrationResultEventsCounter = new AtomicInteger();
        integrationErrorEventProduced = new AtomicBoolean();
        integrationErrorReference = new AtomicReference<>();
    }
}
