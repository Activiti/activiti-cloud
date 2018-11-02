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

package org.activiti.cloud.qa.helper;

import java.util.HashMap;

public class ProcessMatcher {

    private static final HashMap <String, String> processWithTasksDefinitionKeys = new HashMap<String, String>(){{
            put("PROCESS_INSTANCE_WITH_VARIABLES","ProcessWithVariables");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED","SingleTaskProcess");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES","SingleTaskProcessUserCandidates");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES","SingleTaskProcessGroupCandidates");
            put("PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO","fixSystemFailure");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP","singletask-b6095889-6177-4b73-b3d9-316e47749a36");
            put("SUB_PROCESS_INSTANCE_WITH_TASK","subprocess-970cb8df-2d4c-482b-a7f8-c19a983c2ef2");
    }};

    private static final HashMap <String, String> processWithNoTasksDefinitionKeys = new HashMap<String, String>(){{
            put("SIMPLE_PROCESS_INSTANCE","SimpleProcess");
            put("CONNECTOR_PROCESS_INSTANCE","ConnectorProcess");
            put("PROCESS_INSTANCE_WITH_CALL_ACTIVITIES","parentproc-8e992556-5785-4ee0-9fe7-354decfea4a8");
    }};

    public static final HashMap <String, String> processDefinitionKeys = new HashMap<String, String>(){{
            putAll(processWithTasksDefinitionKeys);
            putAll(processWithNoTasksDefinitionKeys);
    }};

    public static String processDefinitionKeyMatcher (String processName){
        return processDefinitionKeys.get(processName);
    }

    public static boolean withTasks(String processName) {
        return processWithTasksDefinitionKeys.containsKey(processName);
    }
}





