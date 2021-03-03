Meta:

Narrative:
As a user
I want to perform operations on process instance having error events

Scenario: check a process instance with boundary error event for subprocess
Given the user is authenticated as hruser
When the user starts a process with error events called ERROR_BOUNDARY_EVENT_SUBPROCESS
Then error events are emitted for the process
And the user can see a task 'Task' with a status CREATED
And the user deletes the process with error events

Scenario: check a process instance with start error event for subprocess
Given the user is authenticated as hruser
When the user starts a process with error events called ERROR_START_EVENT_SUBPROCESS
Then error events are emitted for the process
And the user can see a task 'Task' with a status CREATED
And the user deletes the process with error events

Scenario: check a process instance with boundary error event for callactivitiy
Given the user is authenticated as hruser
When the user starts a process with error events called ERROR_BOUNDARY_EVENT_CALLACTIVITY
Then error events are emitted for the process
And the user can see a task 'Task' with a status CREATED
And the user deletes the process with error events
