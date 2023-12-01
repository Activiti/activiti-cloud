Meta:

Narrative:
As a user
I want to search user and groups

Scenario: search groups by name
Given the user is authenticated as testuser
When the user searches  for groups containing sa
Then group search contains sales
And group search contains processadmin

Scenario: search users by name
Given the user is authenticated as testuser
When the user searches for users containing user
Then user search contains hruser
And user search contains testuser
And user search does not contain testActivitiAdmin
