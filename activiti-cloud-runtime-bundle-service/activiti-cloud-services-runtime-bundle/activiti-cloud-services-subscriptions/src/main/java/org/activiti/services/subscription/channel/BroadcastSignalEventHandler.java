/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.services.subscription.channel;

import java.util.function.Consumer;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.RuntimeService;
import org.springframework.resilience.annotation.Retryable;

public class BroadcastSignalEventHandler implements Consumer<SignalPayload> {

    private final RuntimeService runtimeService;

    public BroadcastSignalEventHandler(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Retryable(
        includes = ActivitiOptimisticLockingException.class,
        maxRetriesString = "${activiti.cloud.subscription.retry.max-attempts:3}",
        delayString = "${activiti.cloud.subscription.retry.backoff.delay:0}"
    )
    @Override
    public void accept(SignalPayload signalPayload) {
        if ((signalPayload.getVariables() == null) || (signalPayload.getVariables().isEmpty())) {
            runtimeService.signalEventReceived(signalPayload.getName());
        } else {
            runtimeService.signalEventReceived(signalPayload.getName(), signalPayload.getVariables());
        }
    }
}
