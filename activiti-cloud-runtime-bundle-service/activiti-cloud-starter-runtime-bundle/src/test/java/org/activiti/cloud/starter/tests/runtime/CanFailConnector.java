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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.messaging.Message;

@TestComponent
public class CanFailConnector {

    private boolean shouldSendError = true;
    private AtomicBoolean integrationErrorSent = new AtomicBoolean(false);
    private IntegrationRequest latestReceivedIntegrationRequest;

    @Autowired
    private IntegrationResultSender integrationResultSender;

    @Autowired
    private IntegrationErrorSender integrationErrorSender;

    public void setShouldSendError(boolean shouldSendError) {
        this.shouldSendError = shouldSendError;
    }

    public void canFailConnector(Message<IntegrationRequest> message) {
        latestReceivedIntegrationRequest = message.getPayload();
        integrationErrorSent.set(false);
        if (shouldSendError) {
            integrationErrorSent.set(true);
            integrationErrorSender.send(message.getPayload(), new RuntimeException("task failed"));
        } else {
            integrationResultSender.send(message.getPayload(), message.getPayload().getIntegrationContext());
        }
    }

    public AtomicBoolean errorSent() {
        return integrationErrorSent;
    }

    public IntegrationRequest getLatestReceivedIntegrationRequest() {
        return latestReceivedIntegrationRequest;
    }
}
