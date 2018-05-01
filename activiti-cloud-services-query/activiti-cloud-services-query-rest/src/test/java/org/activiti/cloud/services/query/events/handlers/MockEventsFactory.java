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

package org.activiti.cloud.services.query.events.handlers;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.query.events.ProcessCancelledEvent;

/**
 * Mock events factory
 */
public class MockEventsFactory {

    /**
     * Create ProcessCancelledEvent
     * @param processInstanceId process instance id
     * @return the created ProcessCancelledEvent
     */
    public static ProcessEngineEvent createProcessCancelledEvent(String processInstanceId) {
        return createProcessCancelledEvent(processInstanceId,
                                           System.currentTimeMillis());
    }

    /**
     * Create ProcessCancelledEvent
     * @param processInstanceId process instance id
     * @param eventTime the timestamp of the event
     * @return the created ProcessCancelledEvent
     */
    public static ProcessEngineEvent createProcessCancelledEvent(String processInstanceId,
                                                                 Long eventTime) {
        return new ProcessCancelledEvent(eventTime,
                                         "ProcessCancelledEvent",
                                         "10",
                                         "100",
                                         processInstanceId,
                                "runtime-bundle-a",
                                "runtime-bundle-a",
                                "runtime-bundle",
                                "1",
                                null,
                                null);
    }

    private MockEventsFactory() {

    }
}
