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

import java.util.concurrent.atomic.AtomicBoolean;
import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

@TestComponent
@EnableBinding(CanFailConnectorChannels.class)
public class CanFailConnector {

    private boolean shouldThrowException = true;
    private AtomicBoolean exceptionThrown = new AtomicBoolean(false);
    private IntegrationRequest latestReceivedIntegrationRequest;

    private final IntegrationResultSender integrationResultSender;

    public CanFailConnector(
        IntegrationResultSender integrationResultSender) {
        this.integrationResultSender = integrationResultSender;
    }

    public void setShouldThrowException(boolean shouldThrowException) {
        this.shouldThrowException = shouldThrowException;
    }

    @StreamListener(value = CanFailConnectorChannels.CAN_FAIL_CONNECTOR)
    public void canFailConnector(IntegrationRequest integrationRequest) {
        latestReceivedIntegrationRequest = integrationRequest;
        exceptionThrown.set(false);
        if (shouldThrowException) {
            exceptionThrown.set(true);
            throw new RuntimeException("The connector 'canFail' is configured to throw an exception");
        }
        integrationResultSender.send(integrationRequest, integrationRequest.getIntegrationContext());
    }

    public AtomicBoolean exceptionThrown() {
        return exceptionThrown;
    }

    public IntegrationRequest getLatestReceivedIntegrationRequest() {
        return latestReceivedIntegrationRequest;
    }
}
