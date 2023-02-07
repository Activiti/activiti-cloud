Meta:

Narrative:
As a user
I want to perform operations on process instance having service tasks

Scenario: audit service tasks integration context events for process instance
Given the user is authenticated as testadmin
When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE
Then integration context events are emitted for the process
And the process with service tasks is completed

Scenario: get service tasks for process instance
Given the user is authenticated as testadmin
When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE
Then the user can get list of service tasks for process instance
And the process with service tasks is completed

Scenario: get service task by id
Given the user is authenticated as testadmin
When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE
Then the user can get service task by id
And the process with service tasks is completed

Scenario: get service task integration context by service task id
Given the user is authenticated as testadmin
When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE
Then the user can get service task integration context by service task id
And the process with service tasks is completed

Scenario: get service tasks by COMPLETED status for process instance
Given the user is authenticated as testadmin
When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE
Then the user can get list of service tasks with status of COMPLETED
And the process with service tasks is completed

Scenario: get service tasks by ERROR status for process instance
Given the user is authenticated as testadmin
And the user provides a variable named var with value test
When the user starts a process with service tasks called BPMN_ERROR_CONNECTOR_PROCESS
Then integration context error events are emitted for the process
And the user can get list of service tasks with status of ERROR
And the status of the process is changed to cancelled

Scenario: get all completed service tasks by query
Given the user is authenticated as testadmin
When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE
Then the user can get list of service tasks for process key ConnectorProcess and status COMPLETED

Scenario: get all error service tasks by query
Given the user is authenticated as testadmin
When the user starts a process with service tasks called BPMN_ERROR_CONNECTOR_PROCESS
Then the user can get list of service tasks for process key testBpmnErrorConnectorProcess and status ERROR

Scenario: replay service task execution with ERROR status
Given the user is authenticated as testadmin
And the user provides a variable named var with value test
When the user starts an instance of process called testErrorConnectorProcess with the provided variables
And integration error event is emitted for the process
And the user can get list of service tasks with status of ERROR
Then the user set the instance variable var with value replay
And the user can replay service task execution
And the user can get list of service tasks with status of STARTED
And all integration context events are emitted for the process
And the user can get list of service tasks with status of COMPLETED
And the process with service tasks is completed
