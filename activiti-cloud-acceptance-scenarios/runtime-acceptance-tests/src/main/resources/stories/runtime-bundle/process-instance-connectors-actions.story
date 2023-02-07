Meta:
Narrative:
As a user
I want to perform operations of processes containing connectors

Scenario: Start a process containing cloud connector
Given the user is authenticated as testuser
And the user provides a variable named movieToRank with value The Lord of The Rings
When the user starts an instance of process called RankMovieId with the provided variables
Then the process instance has a variable named movieToRank with value The Lord of The Rings
And the process instance has a variable named movieDesc with value The Lord of the Rings is an epic high fantasy novel written by English author and scholar J. R. R. Tolkien
And the process instance has a task named Add Rating

Scenario: Complete a process containing multi-instance cloud connector
Given the user is authenticated as testuser
And the user provides an integer variable named instanceCount with value 3
When the user starts an instance of process called miParallelCloudConnector with the provided variables
Then the query process instance has an integer variable named instanceCount with value 3
And the process instance has a resultCollection named miResult with the following integer entries:
|name|value|
|executionCount|1|
|executionCount|2|
|executionCount|3|
And the status of the process is changed to completed

Scenario: Propagate integration error for a process containing cloud connector to audit
Given the user is authenticated as testuser
And the user provides a variable named var with value test
When the user starts an instance of process called testErrorConnectorProcess with the provided variables
Then integration error event is emitted for the process

Scenario: Propagate cloud bpmn error for a process containing cloud connector to audit
Given the user is authenticated as testuser
And the user provides a variable named var with value test
When the user starts an instance of process called testBpmnErrorConnectorProcess with the provided variables
Then cloud bpmn error event is emitted for the process
And the status of the process is changed to cancelled
