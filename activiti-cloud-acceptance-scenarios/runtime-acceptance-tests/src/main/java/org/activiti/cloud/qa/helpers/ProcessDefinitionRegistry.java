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
package org.activiti.cloud.qa.helpers;

import java.util.HashMap;

public class ProcessDefinitionRegistry {

    private static final HashMap<String, String> processWithTasksDefinitionKeys = new HashMap<String, String>() {
        {
            put("PROCESS_INSTANCE_WITH_VARIABLES", "ProcessWithVariables");
            put("TWO_TASK_PROCESS", "twoTaskProcess");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED", "SingleTaskProcess");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES", "SingleTaskProcessUserCandidates");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES", "SingleTaskProcessGroupCandidates");
            put("PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO", "fixSystemFailure");
            put(
                "PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP",
                "singletask-b6095889-6177-4b73-b3d9-316e47749a36"
            );
            put("SUB_PROCESS_INSTANCE_WITH_TASK", "subprocess-970cb8df-2d4c-482b-a7f8-c19a983c2ef2");
            put("PROCESS_INSTANCE_WITH_EMBEDDED_SUB_PROCESS", "startSimpleSubProcess");
            put("PROCESS_WITH_HEADERS_CONNECTOR", "HeadersConnectorProcess");
            put("PROCESS_INSTANCE_WITH_INCLUSIVE_GATEWAY", "basicInclusiveGateway");
            put("TASK_DATE_VAR_MAPPING", "taskDateVarMapping");
            put("PROCESS_START_EVENT_VARIABLE_MAPPING", "process-b42a166d-605b-4eec-8b96-82b1253666bf");
        }
    };

    private static final HashMap<String, String> processWithNoTasksDefinitionKeys = new HashMap<String, String>() {
        {
            put("SIMPLE_PROCESS_INSTANCE", "SimpleProcess");
            put("CONNECTOR_PROCESS_INSTANCE", "ConnectorProcess");
            put("SIGNAL_THROW_PROCESS_INSTANCE", "SignalThrowEventProcess");
            put("SIGNAL_START_EVENT_PROCESS", "SignalStartEventProcess");
            put("PROCESS_INSTANCE_WITH_CALL_ACTIVITIES", "parentproc-8e992556-5785-4ee0-9fe7-354decfea4a8");
            put("Process Information", "processinf-4e42752c-cc4d-429b-9528-7d3df24a9537");
            put("Process with Generic BPMN Task", "processwit-c6fd1b26-0d64-47f2-8d04-0b70764444a7");
            put("PARENT_PROCESS", "parentproc-843144bc-3797-40db-8edc-d23190b118e5");
        }
    };

    private static final HashMap<String, String> processWithTimerEvents = new HashMap<String, String>() {
        {
            put("INTERMEDIATE_TIMER_EVENT_PROCESS", "intermediateTimerEventExample");
            put("START_TIMER_EVENT_PROCESS", "startTimerEventExample");
            put("BOUNDARY_TIMER_EVENT_PROCESS", "boundaryTimerEventExample");
        }
    };

    private static final HashMap<String, String> processWithErrorEvents = new HashMap<String, String>() {
        {
            put("ERROR_BOUNDARY_EVENT_SUBPROCESS", "errorBoundaryEventSubProcess");
            put("ERROR_START_EVENT_SUBPROCESS", "errorStartEventSubProcess");
            put("ERROR_BOUNDARY_EVENT_CALLACTIVITY", "catchErrorOnCallActivity");
            put("BPMN_ERROR_CONNECTOR_PROCESS", "testBpmnErrorConnectorProcess");
        }
    };

    public static final HashMap<String, String> processDefinitionKeys = new HashMap<String, String>() {
        {
            putAll(processWithTasksDefinitionKeys);
            putAll(processWithNoTasksDefinitionKeys);
            putAll(processWithTimerEvents);
            putAll(processWithErrorEvents);
        }
    };

    public static String processDefinitionKeyMatcher(String processName) {
        return processDefinitionKeys.get(processName);
    }

    public static boolean withTasks(String processName) {
        return processWithTasksDefinitionKeys.containsKey(processName);
    }
}
