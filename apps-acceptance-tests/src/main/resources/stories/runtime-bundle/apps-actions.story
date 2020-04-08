Meta:

Narrative:
As a user
I want to see the status of deployed apps
So that I can know the status of deployed apps

Scenario: display the status of deployed apps
Given the user is authenticated as testuser
And the app service is up
And an app is running
Then the status of the app is shown as running