Meta:

Narrative:
As a user
I want to perform an operation on tasks
So that I can get results on a running process

Scenario: claim and complete tasks in a running process
Given the user is authenticated
When the user starts a random process
And the user claims a task
And the user completes the task
Then the status of the process is changed to completed

