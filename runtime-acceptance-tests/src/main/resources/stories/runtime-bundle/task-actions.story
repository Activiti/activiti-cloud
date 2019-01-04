Meta:

Narrative:
As a user
I want to perform an operation on tasks
So that I can get results on a running process

Scenario: claim and complete tasks in a running process
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And the user claims the task
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: create a standalone task
Given the user is authenticated as testuser
When the user creates a standalone task
Then the task is created and the status is assigned

Scenario: delete a standalone task
Given the user is authenticated as testuser
And the user creates a standalone task
When the user deletes the standalone task
Then the standalone task is deleted

Scenario: create a subtask
Given the user is authenticated as testuser
When the user creates a standalone task
And user creates a subtask for the previously created task
Then the subtask is created and references another task

Scenario: get a list of subtasks
Given the user is authenticated as testuser
When the user creates a standalone task
And user creates a subtask for the previously created task
Then a list of one subtask is be available for the task

Scenario: create a process with assigned tasks and complete it
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And the status of the task since the beginning is ASSIGNED
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: create a process with user candidates, claim a task and complete it
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES
And the status of the task is CREATED
And the user claims the task
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: create a process with group candidates, claim a task and complete it
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES
And the status of the task is CREATED
And the user claims the task
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: cannot complete a task that has already been completed
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And the user completes the task
Then the user cannot complete the task

Scenario: cannot claim a task that belongs to different candidate group
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP
And another user is authenticated as hruser
Then the task cannot be claimed by user

Scenario: cannot claim a task that has already been claimed
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES
And the status of the task is CREATED
And the user claims the task
And the status of the task is ASSIGNED
And another user is authenticated as hruser
Then the task cannot be claimed by user

Scenario: cannot see tasks that belongs to different candidate group
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP
And the user claims the task
And the status of the task is ASSIGNED
And the user completes the task
And the status of the task is COMPLETED
And another user is authenticated as hruser
Then tasks of PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP cannot be seen by user

Scenario: subprocess task is created when starting a parent process with call activities
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_CALL_ACTIVITIES
Then the task from SUB_PROCESS_INSTANCE_WITH_TASK is CREATED and it is called subprocess-task

Scenario: check the presence of formKey field in task
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
Then the tasks has the formKey field

Scenario: tasks have their own copies of variables
Given the user is authenticated as testuser
When the user starts with variables for TWO_TASK_PROCESS with variables start1 and start2
And a task variable was created with name start1
And task variable start1 has value start1
And a task variable was created with name start2
And we set task variable start1 to start1modified
And task variable start1 has value start1modified
And the user claims the task
And the user completes the task
And another user is authenticated as hruser
Then task variable start1 has value start1

Scenario: check the task is updated
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And the user updates the updatable fields of the task
Then the task is updated
And the task has the updated fields

