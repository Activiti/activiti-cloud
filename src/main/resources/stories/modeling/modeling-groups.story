Meta:

Narrative:
As a user
I want to create a group

Scenario: create a group
Given any authenticated user
When the user creates a group 'HR Department'
Then the group 'HR Department' is created

