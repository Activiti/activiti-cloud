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
    VARIABLE_UPDATED;
}