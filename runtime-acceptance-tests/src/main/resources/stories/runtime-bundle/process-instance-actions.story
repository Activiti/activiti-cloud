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
Then the diagram is shown

Scenario: show diagram for a process instance without graphic info
Given any authenticated user
When the user starts a process without graphic info
And open the process diagram
Then no diagram is shown