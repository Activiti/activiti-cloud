Meta:

Narrative:
As an hradmin
I want to perform several authorised and unauthorised actions
So that I can check the enforcement of security policies

Scenario: getting process instances
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user can get process with variables instances in admin endpoint

Scenario: querying process instances
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user can query process with variables instances in admin endpoints

Scenario: getting events
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user can get events for process with variables instances in admin endpoint

Scenario:
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user cannot get process with variables instances
And the user cannot query process with variables instances
And the user cannot get events for process with variables instances
