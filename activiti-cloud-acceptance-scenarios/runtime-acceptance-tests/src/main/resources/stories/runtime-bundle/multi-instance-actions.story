Meta:

Narrative:
As a user
I want to perform operations on processes containing multi-instances

Scenario: collect all the variables from a multi-instantiated user task
Given the user is authenticated as testuser
When the user starts an instance of the process with key miParallelUserTasksAllOutputCollection
And the user completes the task available in the current process instance passing the following variables:
|name|value|
|meal|pizza|
|size|large|
And the user completes the task available in the current process instance passing the following variables:
|name|value|
|meal|pasta|
|size|medium|
Then the process instance reaches a task named Wait
And the process instance has a resultCollection named miResult with entries of size 3 as following:
|name|value|
|meal|pizza|
|size|large|
|sys_task_assignee|testuser|
|meal|pasta|
|size|medium|
|sys_task_assignee|testuser|

Scenario: execute multi-instance service task and validate its status in the audit log
Given the user is authenticated as testuser
When the user starts an instance of the process with key Process_B-f96qb1
Then the generated ACTIVITY_COMPLETED events for activity ServiceTask_0qp1237 have the expected count of 3
And the process with service tasks is completed
