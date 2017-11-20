/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.starters.test;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;

import static org.activiti.cloud.starters.test.MockProcessEngineEvent.anIntegrationRequestSentEvent;

public class MockEventsSamples {

    public static List<ProcessEngineEvent> allSupportedEvents() {
        List<ProcessEngineEvent> coveredEvents = new ArrayList<>();
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "ActivityCancelledEvent",
                                                     "100",
                                                     "103",
                                                     "104"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "ActivityStartedEvent",
                                                     "2",
                                                     "3",
                                                     "4"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "ActivityCompletedEvent",
                                                     "11",
                                                     "23",
                                                     "42"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "ProcessCompletedEvent",
                                                     "12",
                                                     "24",
                                                     "43"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "ProcessCancelledEvent",
                                                     "112",
                                                     "124",
                                                     "143"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "ProcessStartedEvent",
                                                     "13",
                                                     "25",
                                                     "44"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "SequenceFlowTakenEvent",
                                                     "14",
                                                     "26",
                                                     "45"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "TaskAssignedEvent",
                                                     "15",
                                                     "27",
                                                     "46"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "TaskCompletedEvent",
                                                     "16",
                                                     "28",
                                                     "47"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "TaskCreatedEvent",
                                                     "17",
                                                     "29",
                                                     "48"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "VariableCreatedEvent",
                                                     "18",
                                                     "30",
                                                     "49"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "VariableDeletedEvent",
                                                     "19",
                                                     "31",
                                                     "50"));
        coveredEvents.add(new MockProcessEngineEvent(System.currentTimeMillis(),
                                                     "VariableUpdatedEvent",
                                                     "20",
                                                     "32",
                                                     "51"));
        coveredEvents.add(anIntegrationRequestSentEvent("21",
                                                        "33",
                                                        "52"));
        return coveredEvents;
    }
}
