# activiti-cloud-api
Activiti Cloud Native APIs &amp; Shared Models.

## Cloud Runtime Events

These events are sent to the `engineEvents` Spring Cloud Stream destination in the runtime-bundle microservice in JSON format.

All events have a set of common Activiti Cloud API properties defined in [org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent](./activiti-cloud-api-model-shared/src/main/java/org/activiti/cloud/api/model/shared/events/CloudRuntimeEvent.java). 

| Name | Description | Type | Example |
|------|-------------|------|---------|
| appName | Application name, for a runtime bundle it is the value of the `activiti.cloud.application.name` spring property. | string | myapp |
| appVersion | Application version, for a runtime-bundle it is the value of the `activiti.cloud.application.version` spring property. | string | 1 |
| serviceName | Service name, for a runtime-bundle it is the value of the `spring.application.name` spring property. | string | rb-myapp |
| serviceFullName | Service full name, at the moment it is the same as serviceName. | string | rb-myapp |
| serviceType | Service type, for a runtime-bundle it is the value of the `activiti.cloud.service.type` spring property. | string | runtime-bundle |
| serviceVersion | Service version, for a runtime-bundle it is the value of the `activiti.cloud.service.version` spring property. | string | |
| sequenceNumber | Sequence index of the event if it is part of an aggregate within the message if part of the same transaction. |
| messageId | ID of the message that carried the event, all the events that are part of the same tx are aggregated in the same message. | string | |
| entityId | ID of the entity included in the message. | string | |

Plus the following common Activiti Core API properties defined in [org.activiti.api.model.shared.event.RuntimeEvent](https://github.com/Activiti/activiti-api/blob/develop/activiti-api-model-shared/src/main/java/org/activiti/api/model/shared/event/RuntimeEvent.java)

| Name | Description | Type | Example |
|------|-------------|------|---------|
| id | event ID | | |
| entity | The entity included in the message | object | |
| timestamp | event timestamp | | |
| eventType | event type | string | |
| processInstanceId | | |
| parentProcessInstanceId | | |
| processDefinitionId | | |
| processDefinitionKey | | |
| processDefinitionVersion | | |
| businessKey | | |

### Cloud Event Types

The following list shows all available cloud event types.

Most of the event types are the same as in the original enum from the internal deprecated old API in Activiti Core, so the same description still applies: [org.activiti.engine.delegate.event.ActivitiEventType](https://github.com/Activiti/Activiti/blob/develop/activiti-engine/src/main/java/org/activiti/engine/delegate/event/ActivitiEventType.java).

A new enum is available for all the Cloud events: [org.activiti.cloud.api.process.events.CloudRuntimeEventType](./activiti-cloud-api-events/src/main/java/org/activiti/cloud/api/events/CloudRuntimeEventType.java)

| Name | Description | Class |
|------|-------------|---------|
| ACTIVITY_STARTED | | [org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNActivityStartedEvent.java) | 
| ACTIVITY_CANCELLED | | [org.activiti.cloud.api.process.model.events.CloudBPMNActivityCancelledEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNActivityCancelledEvent.java) | 
| ACTIVITY_COMPLETED | | [org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNActivityCompletedEvent.java) |
| ERROR_RECEIVED | | [org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNErrorReceivedEvent.java) |
| SIGNAL_RECEIVED | | [org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNSignalReceivedEvent.java) |
| TIMER_FIRED | | [org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNTimerFiredEvent.java) |
| TIMER_CANCELLED | | [org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNTimerCancelledEvent.java) |
| TIMER_SCHEDULED | | [org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNTimerScheduledEvent.java) |
| TIMER_EXECUTED | | [org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNTimerExecutedEvent.java) |
| TIMER_FAILED | | [org.activiti.cloud.api.process.model.events.CloudBPMNTimerFailedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNTimerFailedEvent.java) |
| TIMER_RETRIES_DECREMENTED | | [org.activiti.cloud.api.process.model.events.CloudBPMNTimerRetriesDecrementedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudBPMNTimerRetriesDecrementedEvent.java) |
| MESSAGE_WAITING | The process reached a message catch event and is listening for a BPMN message, see ACTIVITY_MESSAGE_WAITING | [org.activiti.cloud.api.process.model.events.CloudBPMNMessageWaitingEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/CloudBPMNMessageWaitingEvent.java) |
| MESSAGE_RECEIVED | see ACTIVITY_MESSAGE_RECEIVED | [org.activiti.cloud.api.process.model.events.CloudBPMNMessageReceivedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/CloudBPMNMessageReceivedEvent.java) |
| MESSAGE_SENT | see ACTIVITY_MESSAGE_SENT | [org.activiti.cloud.api.process.model.events.CloudBPMNMessageSentEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/CloudBPMNMessageSentEvent.java) |
| INTEGRATION_REQUESTED | the runtime bundle has sent a request to a cloud connector | [org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudIntegrationRequestedEvent.java) |
| INTEGRATION_RESULT_RECEIVED | the runtime bundle has received a result from a cloud connector | [org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudIntegrationResultReceivedEvent.java) |
| PROCESS_DEPLOYED | | [org.activiti.cloud.api.process.model.events.CloudProcessDeployedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessDeployedEvent.java)|
| PROCESS_CREATED | | [org.activiti.cloud.api.process.model.events.CloudProcessCreatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessCreatedEvent.java)|
| PROCESS_STARTED | | [org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessStartedEvent.java) |
| PROCESS_COMPLETED | | [org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessCompletedEvent.java) |
| PROCESS_CANCELLED | | [org.activiti.cloud.api.process.model.events.CloudProcessCancelledEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessCancelledEvent.java) |
| PROCESS_SUSPENDED | | [org.activiti.cloud.api.process.model.events.CloudProcessSuspendedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessSuspendedEvent.java) |
| PROCESS_RESUMED | | [org.activiti.cloud.api.process.model.events.CloudProcessResumedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessResumedEvent.java) |
| PROCESS_UPDATED | | [org.activiti.cloud.api.process.model.events.CloudProcessUpdatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudProcessUpdatedEvent.java) |
| SEQUENCE_FLOW_TAKEN | | [org.activiti.cloud.api.process.model.events.CloudSequenceFlowTakenEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudSequenceFlowTakenEvent.java) |
| START_MESSAGE_DEPLOYED | Similar to MESSAGE_WAITING, but for start message events. | [org.activiti.cloud.api.process.model.events.CloudStartMessageDeployedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudStartMessageDeployedEvent.java) |
| MESSAGE_SUBSCRIPTION_CANCELLED | A message event subscription entity being deleted, i.e. when a running process is deleted with an active catch message event activity or after a catch message event activity triggered by corresponding throw message. | [org.activiti.cloud.api.process.model.events.CloudMessageSubscriptionCancelledEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudMessageSubscriptionCancelledEvent.java) |
| TASK_CREATED | | [org.activiti.cloud.api.process.model.events.CloudTaskCreatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCreatedEvent.java) |
| TASK_UPDATED | | [org.activiti.cloud.api.process.model.events.CloudTaskUpdatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskUpdatedEvent.java) |
| TASK_ASSIGNED | | [org.activiti.cloud.api.process.model.events.CloudTaskAssignedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskAssignedEvent.java) |
| TASK_COMPLETED | | [org.activiti.cloud.api.process.model.events.CloudTaskCompletedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCompletedEvent.java) |
| TASK_SUSPENDED | | [org.activiti.cloud.api.process.model.events.CloudTaskSuspendedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskSuspendedEvent.java) |
| TASK_ACTIVATED | | [org.activiti.cloud.api.process.model.events.CloudTaskActivatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskActivatedEvent.java) |
| TASK_CANCELLED | | [org.activiti.cloud.api.process.model.events.CloudTaskCancelledEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCancelledEvent.java) |
| TASK_CANDIDATE_USER_ADDED | | [org.activiti.cloud.api.process.model.events.CloudTaskCandidateUserAddedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCandidateUserAddedEvent.java) |
| TASK_CANDIDATE_USER_REMOVED | | [org.activiti.cloud.api.process.model.events.CloudTaskCandidateUserRemovedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCandidateUserRemovedEvent.java) |
| TASK_CANDIDATE_GROUP_ADDED | | [org.activiti.cloud.api.process.model.events.CloudTaskCandidateGroupAddedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCandidateGroupAddedEvent.java) |
| TASK_CANDIDATE_GROUP_REMOVED | | [org.activiti.cloud.api.process.model.events.CloudTaskCandidateGroupRemovedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudTaskCandidateGroupRemovedEvent.java) |
| VARIABLE_CREATED | | [org.activiti.cloud.api.process.model.events.CloudVariableCreatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudVariableCreatedEvent.java) |
| VARIABLE_UPDATED | | [org.activiti.cloud.api.process.model.events.CloudVariableUpdatedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudVariableUpdatedEvent.java) |
| VARIABLE_DELETED | | [org.activiti.cloud.api.process.model.events.CloudVariableDeletedEvent](./activiti-cloud-api-process-model/src/main/java/org/activiti/cloud/api/process/model/events/CloudVariableDeletedEvent.java) |

#### JSON Schema

For a detailed view of event contents refer to the [Activiti Cloud Events JSON schemas](./activiti-cloud-api-events/src/main/resources/org/activiti/cloud/api/events/schema).
*NB* these schema files are generated running the test [JsonSchemaGeneratorTest](./activiti-cloud-api-events/src/test/java/org/activiti/cloud/api/events/schema/JsonSchemaGeneratorTest.java).
