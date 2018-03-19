Meta:

Narrative:
As a user
I want to perform operations on process instances

Scenario: delete a process instance
Given any authenticated user
When the user starts a random process
!--And cancel the process
!--Then the process instance is cancelled

Scenario: show a process instance diagram
Given any authenticated user
When the user starts a random process
And open the process diagram
Then the digram is shown

