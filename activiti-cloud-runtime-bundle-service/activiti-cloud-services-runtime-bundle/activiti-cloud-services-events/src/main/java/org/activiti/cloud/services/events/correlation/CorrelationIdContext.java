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
package org.activiti.cloud.services.events.correlation;

import java.util.UUID;
import java.util.function.Supplier;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;

public final class CorrelationIdContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdContext() {}

    public static ProcessInstance startWithCorrelationId(
        StartProcessPayload startProcessPayload,
        Supplier<ProcessInstance> startFn
    ) {
        String correlationId = ensure(startProcessPayload);
        try {
            ProcessInstance processInstance = startFn.get();
            if (
                processInstance instanceof ProcessInstanceImpl processInstanceImpl &&
                processInstanceImpl.getCorrelationId() == null
            ) {
                processInstanceImpl.setCorrelationId(correlationId);
            }
            return processInstance;
        } finally {
            clear();
        }
    }

    public static String consume() {
        String correlationId = CURRENT.get();
        CURRENT.remove();
        return correlationId;
    }

    private static String ensure(StartProcessPayload startProcessPayload) {
        String correlationId = startProcessPayload.getCorrelationId();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            startProcessPayload.setCorrelationId(correlationId);
        }
        CURRENT.set(correlationId);
        return correlationId;
    }

    private static void clear() {
        CURRENT.remove();
    }
}
