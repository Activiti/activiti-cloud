Meta:

Narrative:
As a user
I want to perform an action
So that I can achieve a business goal

Scenario: delete records in query service
Given the user is authenticated as testadmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
Then the user is able to delete all process instances in query service
And the user is able to delete all tasks in query service