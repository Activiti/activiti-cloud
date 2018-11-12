Meta:

Narrative:
As a user
I want to perform an action
So that I can achieve a business goal

Scenario: start a process
Given the user is authenticated as testuser
When A - the user starts a Process Information
Then the process is completed

Scenario: check information of the process
Given the user is authenticated as testuser
When B - the user starts a Process Information with name processInstanceName and businessKey businessKey
Then the process instance information is correctly propagated

Scenario: check events in process with variables
Given the user is authenticated as testuser
When C - the user starts a Process Information with variables
Then the events are as expected