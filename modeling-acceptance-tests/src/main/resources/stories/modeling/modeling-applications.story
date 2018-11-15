Meta:

Narrative:
As a user
I want to manage applications

Scenario: create an application
Given the user is authenticated as modeler
When the user creates an application 'Mars Team'
Then the application 'Mars Team' is created

Scenario: update an application
Given the user is authenticated as modeler
And an existing application 'Mars Team'
When the user opens the application 'Mars Team'
And update the application name to 'Mars Terraforming Team'
Then the application name is updated to 'Mars Terraforming Team'

Scenario: delete an application
Given the user is authenticated as modeler
And an existing application 'Mars Team'
When the user delete the application 'Mars Team'
Then the application 'Mars Team' is deleted
