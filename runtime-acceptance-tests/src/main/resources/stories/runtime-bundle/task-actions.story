Meta:

Narrative:
As a user
I want to perform an operation on tasks
So that I can get results on a running process

Scenario: claim and complete tasks in a running process
Given the user is authenticated as testuser
When the user starts a process with variables
And the testuser claims the task
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: create a standalone task
Given the user is authenticated as testuser
When the user creates a standalone task
Then the task is created and the status is assigned

Scenario: delete a standalone task
Given the user is authenticated as testuser
And an existing standalone task
When the user cancel the task
Then the task is cancelled

Scenario: create a subtask
Given the user is authenticated as testuser
When the user creates a standalone task
And user creates a subtask for the previously created task
Then the subtask is created and references another task

Scenario: get a list of subtasks
Given the user is authenticated as testuser
When the user creates a standalone task
And user creates a subtask for the previously created task
Then a list of one subtask should be available for the task

Scenario: create a process with assigned tasks and complete it
Given the user is authenticated as testuser
When the user starts a single-task process
And the status of the task is ASSIGNED
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: create a process with user candidates, claim a task and complete it
Given the user is authenticated as testuser
When the user starts a single-task process with user candidates
And the status of the task is CREATED
And the testuser claims the task
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: create a process with group candidates, claim a task and complete it
Given the user is authenticated as testuser
When the user starts a single-task process with group candidates
And the status of the task is CREATED
And the testuser claims the task
And the user completes the task
Then the status of the process and the task is changed to completed

Scenario: cannot complete a task that has already been completed
Given the user is authenticated as testuser
When the user starts a single-task process
And the user completes the task
Then the user cannot complete the task

Scenario: cannot claim a task that has already been claimed
Given the user is authenticated as testuser
When the user starts a single-task process with user candidates
And the status of the task is CREATED
And the testuser claims the task
And another user is authenticated as hruser
Then the task cannot be claimed by hruser



























