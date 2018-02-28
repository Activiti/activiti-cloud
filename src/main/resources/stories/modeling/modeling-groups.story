Meta:

Narrative:
As a user
I want to create groups

Lifecycle:
After:
Then delete groups 'Aliens, Recruiting, HR Department'

Scenario: create a group
Given any authenticated user
When the group 'Aliens' does't exists
And the user creates a group 'Aliens'
Then the group 'Aliens' is created

Scenario: create a subgroup
Given any authenticated user
And an existing group 'HR Department'
When the user opens the group 'HR Department'
And creates a group 'Recruiting'
Then the subgroup 'Recruiting' is created in the current 'HR Department' group
