Meta:

Narrative:
As an hruser
I want to perform several authorised and unauthorised actions
So that I can check the enforcement of security policies

Scenario: starting an instance of a simple process
Given the user is authenticated as hruser
When the user starts an instance of the process called SIMPLE_PROCESS_INSTANCE
Then the user can get simple process instances
And the user can query simple process instances
And the user can get events for simple process instances

Scenario: starting an instance of a process with variables
Given the user is authenticated as hruser
Then the user cannot start the process with variables

Scenario: getting process instances and tasks
Given the user is authenticated as hruser
Then the user cannot get process with variables instances
And the user can get tasks

Scenario: querying instances
Given the user is authenticated as hruser
Then the user cannot query process with variables instances

Scenario: querying tasks
Given the user is authenticated as hruser
Then the user can query tasks

Scenario: getting events in audit
Given the user is authenticated as hruser
Then the user cannot get events for process with variables instances
