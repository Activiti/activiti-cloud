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
package org.activiti.cloud.api.events;

/**
 * Enumeration for all the Activiti Cloud event types.
 */
public enum CloudRuntimeEventType {
    ACTIVITY_CANCELLED,
    ACTIVITY_COMPLETED,
    ACTIVITY_STARTED,
    ERROR_RECEIVED,
    /**
     * The runtime bundle has sent a request to a cloud connector.
     */
    INTEGRATION_REQUESTED,
    /**
     * The runtime bundle has received a result from a cloud connector.
     */
    INTEGRATION_RESULT_RECEIVED,
    /**
     * The runtime bundle has received a error from a cloud connector.
     */
    INTEGRATION_ERROR_RECEIVED,
    MESSAGE_RECEIVED,
    MESSAGE_SENT,
    MESSAGE_SUBSCRIPTION_CANCELLED,
    /**
     * The process reached a message catch event and is listening for a BPMN message.
     */
    MESSAGE_WAITING,
    PROCESS_CANCELLED,
    PROCESS_COMPLETED,
    PROCESS_CREATED,
    PROCESS_DEPLOYED,
    PROCESS_RESUMED,
    PROCESS_STARTED,
    PROCESS_SUSPENDED,
    PROCESS_UPDATED,
    PROCESS_DELETED,
    SEQUENCE_FLOW_TAKEN,
    SIGNAL_RECEIVED,
    /**
     * Similar to MESSAGE_WAITING, but for start message events.
     */
    START_MESSAGE_DEPLOYED,
    TASK_ACTIVATED,
    TASK_ASSIGNED,
    TASK_CANCELLED,
    TASK_CANDIDATE_GROUP_ADDED,
    TASK_CANDIDATE_GROUP_REMOVED,
    TASK_CANDIDATE_USER_ADDED,
    TASK_CANDIDATE_USER_REMOVED,
    TASK_COMPLETED,
    TASK_CREATED,
    TASK_SUSPENDED,
    TASK_UPDATED,
    TIMER_CANCELLED,
    TIMER_EXECUTED,
    TIMER_FAILED,
    TIMER_FIRED,
    TIMER_RETRIES_DECREMENTED,
    TIMER_SCHEDULED,
    VARIABLE_CREATED,
    VARIABLE_DELETED,
    VARIABLE_UPDATED,
    /**
     * The runtime bundle has deployed an application
     */
    APPLICATION_DEPLOYED;
}
