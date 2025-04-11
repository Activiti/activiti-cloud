Meta:

Narrative:
As a user
I want to perform operations on process definitions

Scenario: as a user I should be able to get process model
Given the user is authenticated as testuser
Then the user can get the process model for process with key SingleTaskProcess by passing its id

Scenario: as a user I should be able to get process diagram
Given the user is authenticated as testuser
Then the process diagram image for process with key bigProcess is the same as process-definition-diagram.result.svg file
