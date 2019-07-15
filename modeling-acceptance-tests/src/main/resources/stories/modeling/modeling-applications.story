Meta:

Narrative:
As a user
I want to manage projects

Scenario: create an project
Given the user is authenticated as modeler
When the user creates a project 'Mars Team'
Then the project 'Mars Team' is created

Scenario: update an project
Given the user is authenticated as modeler
And an existing project 'Mars Team'
When the user opens the project 'Mars Team'
And update the project name to 'Mars Terraforming Team'
Then the project name is updated to 'Mars Terraforming Team'

Scenario: delete an project
Given the user is authenticated as modeler
And an existing project 'Mars Team'
When the user delete the project 'Mars Team'
Then the project 'Mars Team' is deleted

Scenario: export an project
Given the user is authenticated as modeler
And an project 'Mission Europa' with process model landing-rover and process variables 'age, gender'
When the user export the project
Then the exported project contains the process model landing-rover with process variables 'age, gender'

Scenario: export an invalid project
Given the user is authenticated as modeler
And an project 'Mission Europa' with process model 'landing-rover-not-executable'
Then the project cannot be exported due to validation errors

Scenario: export an invalid project containing user task with no assignee
Given the user is authenticated as modeler
And an project 'Mission Europa' with process model 'spacex-usertask-with-no-assignee'
Then the project cannot be exported due to validation errors with message "One of the attributes 'assignee','candidateUsers' or 'candidateGroups' are mandatory on user task"