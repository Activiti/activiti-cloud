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

//    public enum ProcessDefinitionKey {
//
//        SIMPLE_PROCESS_INSTANCE ("SimpleProcess"),
//        CONNECTOR_PROCESS_INSTANCE("ConnectorProcess"),
//        PROCESS_INSTANCE_WITH_VARIABLES("ProcessWithVariables"),
//        PROCESS_INSTANCE_WITH_SINGLE_TASK("SingleTaskProcess"),
//        PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES("SingleTaskProcessUserCandidates"),
//        PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES("SingleTaskProcessGroupCandidates"),
//        PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO("fixSystemFailure"),
//        PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP("singletask-b6095889-6177-4b73-b3d9-316e47749a36");
//
//        private String processDefinitionKey;
//        ProcessDefinitionKey(String processDefinitionKey){
//            this.processDefinitionKey = processDefinitionKey;
//        }
//        public String getProcessDefinitionKey(){
//            return this.processDefinitionKey;
//        }
//    }
    private static final HashMap <String, String> processWithTasksDefinitionKeys = new HashMap<String, String>(){{
            put("PROCESS_INSTANCE_WITH_VARIABLES","ProcessWithVariables");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK","SingleTaskProcess");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES","SingleTaskProcessUserCandidates");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES","SingleTaskProcessGroupCandidates");
            put("PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO","fixSystemFailure");
            put("PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP","singletask-b6095889-6177-4b73-b3d9-316e47749a36");
    }};

    private static final HashMap <String, String> processWithNoTasksDefinitionKeys = new HashMap<String, String>(){{
            put("SIMPLE_PROCESS_INSTANCE","SimpleProcess");
            put("CONNECTOR_PROCESS_INSTANCE","ConnectorProcess");
    }};

    public static final HashMap <String, String> processDefinitionKeys = new HashMap<String, String>(){{
            putAll(processWithTasksDefinitionKeys);
            putAll(processWithNoTasksDefinitionKeys);
    }};

    public static String processDefinitionKeyMatcher (String processName){
        return processDefinitionKeys.get(processName);
    }

//    processDefinitionKeys.get("SIMPLE_PROCESS_INSTANCE")
//
//            processDefinitionKeys.get("PROCESS_INSTANCE_WITH_VARIABLES")


//    public static String processDefinitionKeyMatcher (String processName){
//
//        String processDefinitionKey;
//
//        switch(processName) {
//            case "process with variables":
//                processDefinitionKey = processDefinitionKeys.get(processName);
//                break;
//            case "single-task process":
//                processDefinitionKey = ProcessDefinitionKey.PROCESS_INSTANCE_WITH_SINGLE_TASK.getProcessDefinitionKey();
//                break;
//            case "single-task process with user candidates":
//                processDefinitionKey = ProcessDefinitionKey.PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES.getProcessDefinitionKey();
//                break;
//            case "single-task process with group candidates":
//                processDefinitionKey = ProcessDefinitionKey.PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES.getProcessDefinitionKey();
//                break;
//            case "process without graphic info":
//                processDefinitionKey = ProcessDefinitionKey.PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO.getProcessDefinitionKey();
//                break;
//            case "connector process":
//                processDefinitionKey = ProcessDefinitionKey.CONNECTOR_PROCESS_INSTANCE.getProcessDefinitionKey();
//                break;
//            case "single-task process with group candidates for test group":
//                processDefinitionKey =  ProcessDefinitionKey.
//                                        PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP
//                                        .getProcessDefinitionKey();
//                break;
//            default:
//                processDefinitionKey = ProcessDefinitionKey.SIMPLE_PROCESS_INSTANCE.getProcessDefinitionKey();
//        }
//
//        return processDefinitionKey;
//    }

    public static boolean withTasks(String processName) {
//        if(processName.equals("connector process") || processName.equals("any"))
//            return false;
//        else
//            return true;

        return processWithTasksDefinitionKeys.containsKey(processName);
    }
}
