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

import org.activiti.cloud.api.process.model.IntegrationRequest;
import org.springframework.boot.test.context.TestComponent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@TestComponent("canFailConnector")
public class CanFailConnector implements Consumer<IntegrationRequest> {

    private boolean shouldSendError = true;
    private AtomicBoolean integrationErrorSent = new AtomicBoolean(false);
    private IntegrationRequest latestReceivedIntegrationRequest;

    private final IntegrationResultSender integrationResultSender;
    private final IntegrationErrorSender integrationErrorSender;

    public CanFailConnector(IntegrationResultSender integrationResultSender,
                            IntegrationErrorSender integrationErrorSender) {
        this.integrationResultSender = integrationResultSender;
        this.integrationErrorSender = integrationErrorSender;
    }

    public void setShouldSendError(boolean shouldSendError) {
        this.shouldSendError = shouldSendError;
    }

    @Override
    public void accept(IntegrationRequest integrationRequest) {
        latestReceivedIntegrationRequest = integrationRequest;
        integrationErrorSent.set(false);
        if (shouldSendError) {
            integrationErrorSent.set(true);
            integrationErrorSender.send(integrationRequest,
                                        new RuntimeException("task failed"));
        } else {
            integrationResultSender.send(integrationRequest,
                integrationRequest.getIntegrationContext());
        }
    }

    public AtomicBoolean errorSent() {
        return integrationErrorSent;
    }

    public IntegrationRequest getLatestReceivedIntegrationRequest() {
        return latestReceivedIntegrationRequest;
    }
}
