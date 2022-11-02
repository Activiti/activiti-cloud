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
package org.activiti.cloud.services.audit.api.converters;

public enum CloudRuntimeEventType {
    ACTIVITY_STARTED,
    ACTIVITY_CANCELLED,
    ACTIVITY_COMPLETED,
    ERROR_RECEIVED,
    SIGNAL_RECEIVED,
    TIMER_SCHEDULED,
    TIMER_FIRED,
    TIMER_CANCELLED,
    TIMER_EXECUTED,
    TIMER_FAILED,
    TIMER_RETRIES_DECREMENTED,
    MESSAGE_WAITING,
    MESSAGE_RECEIVED,
    MESSAGE_SENT,
    INTEGRATION_REQUESTED,
    INTEGRATION_RESULT_RECEIVED,
    INTEGRATION_ERROR_RECEIVED,
    PROCESS_DEPLOYED,
    PROCESS_CREATED,
    PROCESS_STARTED,
    PROCESS_COMPLETED,
    PROCESS_CANCELLED,
    PROCESS_SUSPENDED,
    PROCESS_RESUMED,
    PROCESS_UPDATED,
    PROCESS_DELETED,
    SEQUENCE_FLOW_TAKEN,
    TASK_CANDIDATE_GROUP_ADDED,
    TASK_CANDIDATE_GROUP_REMOVED,
    TASK_CANDIDATE_USER_ADDED,
    TASK_CANDIDATE_USER_REMOVED,
    TASK_ASSIGNED,
    TASK_COMPLETED,
    TASK_CREATED,
    TASK_UPDATED,
    TASK_ACTIVATED,
    TASK_SUSPENDED,
    TASK_CANCELLED,
    VARIABLE_CREATED,
    VARIABLE_UPDATED,
    VARIABLE_DELETED,
    APPLICATION_DEPLOYED,
    PROCESS_CANDIDATE_STARTER_USER_ADDED,
    PROCESS_CANDIDATE_STARTER_USER_REMOVED,
    PROCESS_CANDIDATE_STARTER_GROUP_ADDED,
    PROCESS_CANDIDATE_STARTER_GROUP_REMOVED
}
