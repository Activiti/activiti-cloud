Meta:

Narrative:
As a user
I want to perform an action
So that I can achieve a business goal

Scenario: task variables synchronization
Given the user is authenticated as testuser
When the user creates a standalone task
And the user creates task variables
Then task variables are visible in rb and query
