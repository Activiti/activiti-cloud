Meta:

Narrative:
As a user
I want to perform an action
So that I can achieve a business goal

Scenario: start a basic process
Given the user is authenticated as testuser
When the user starts a process called Process Information
Then the process is completed
And the events are as expected for Process Information

Scenario: check events in basic process with variables
Given the user is authenticated as testuser
When the user starts a process with variables called Process Information
Then the events are as expected for Process Information with variables

Scenario: check the propagation of information of the basic process
Given the user is authenticated as testuser
When the user starts a process called Process Information
Then the process instance information is correctly propagated