Meta:

Narrative:
As a user
I want to perform operations on process instance variables

Scenario: variable types are correct for variables
Given the user is authenticated as hruser
When services are started
When the user starts the process TASK_DATE_VAR_MAPPING
And the process variables are created
And variables have correct values
And variables have correct types in rb
And check variables in query
And variables was created event in audit
And variables values created in task with variable mapping are correct
And variables types in task are correct
When the user ask to claim the task
When update task variables
And the user ask to complete the task
Then variables have correct values in process

Scenario: variables mapping for the process start event
Given the user is authenticated as hruser
When the user starts variables mapping process on start event
Then process variables are properly mapped on start event
And process variables are properly mapped to the task variables
And the user may complete the task
