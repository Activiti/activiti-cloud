Meta:

Narrative:
As an hruser
I want to perform several authorised and unauthorised actions
So that I can check the enforcement of security policies

Scenario: starting an instance of a process with variables
Given the user is authenticated as an hruser
Then the user cannot start the process with variables

Scenario: getting process instances and tasks
Given the user is authenticated as an hruser
Then the user cannot get process with variables instances
And the user can get tasks

Scenario: getting instances in query
Given the user is authenticated as an hruser
Then the user cannot query process with variables instances

Scenario: getting tasks in query
Given the user is authenticated as an hruser
Then the user can query tasks

Scenario: getting events
Given the user is authenticated as an hruser
Then the user cannot get events for process with variables instances


Scenario: starting an instance of a simple process
Given the user is authenticated as an hruser
When the user starts a simple process
Then the user can get simple process instances
And the user can query simple process instances
And the user can get events for simple process instances


Scenario: test
Given the number is "googol"



