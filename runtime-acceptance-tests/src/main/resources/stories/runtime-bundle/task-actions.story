Meta:

Narrative:
As a user
I want to perform an operation on tasks
So that I can get results on a running process

Scenario: claim and complete tasks in a running process
Given the user is authenticated as a testuser
When the user starts a process with variables
And the user claims a task
And the user completes the task
Then the status of the process is changed to completed

Scenario: create a standalone task
Given the user is authenticated as a testuser
When the user creates a standalone task
Then the task is created and the status is assigned

Scenario: delete a standalone task
Given the user is authenticated as a testuser
And an existing standalone task
When the user cancel the task
Then the task is cancelled

Scenario: create a subtask
Given the user is authenticated as a testuser
When the user creates a standalone task
And user creates a subtask for the previously created task
Then the subtask is created and references another task

Scenario: get a list of subtasks
Given the user is authenticated as a testuser
When the user creates a standalone task
And user creates a subtask for the previously created task
Then a list of one subtask should be available for the task

