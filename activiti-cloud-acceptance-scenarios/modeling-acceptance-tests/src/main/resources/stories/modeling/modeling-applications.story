Meta:

Narrative:
As a user
I want to manage projects

Scenario: create an project
Given the user is authenticated as modeler
When the user creates a project 'mars-team'
Then the project 'mars-team' is created

Scenario: update an project
Given the user is authenticated as modeler
And an existing project 'mars-team'
When the user opens the project 'mars-team'
And update the project name to 'mars-terraforming-team'
Then the project name is updated to 'mars-terraforming-team'

Scenario: delete an project
Given the user is authenticated as modeler
And an existing project 'mars-team'
When the user delete the project 'mars-team'
Then the project 'mars-team' is deleted

Scenario: export an project
Given the user is authenticated as modeler
And an project 'mission-europe' with process model landing-rover and process variables 'age, gender'
When the user export the project
Then the exported project contains the process model landing-rover with process variables 'age, gender'

Scenario: export an invalid project
Given the user is authenticated as modeler
And an project 'mission-europe' with process model 'landing-rover-not-executable'
Then the project can be exported due to validation errors

Scenario: the project should contain validation errors
Given the user is authenticated as modeler
And an project 'mission-europe' with process model 'landing-rover-not-executable'
Then the project should contain validation errors

Scenario: validate an invalid project containing user task with no assignee
Given the user is authenticated as modeler
And an project 'mission-europe' with process model 'spacex-usertask-with-no-assignee'
Then the project should have validation errors with message "One of the attributes 'assignee','candidateUsers' or 'candidateGroups' are mandatory on user task with id: 'UserTask_1tfjds5' and empty name"

Scenario: validate an invalid project containing service task with invalid implementation
Given the user is authenticated as modeler
And an project 'mission-europe' with process model 'spacex-service-task-with-invalid-implementation'
Then the project should have validation errors with message "Invalid service implementation on service 'ServiceTask_1qr4ad0'"

Scenario: get project filtering by name
Given the user is authenticated as modeler
And existing projects 'mars-team, mars-terraforming-team, terra-discovery-team'
When the user gets the projects by name 'terra'
Then the retrieved projects are 'mars-terraforming-team, terra-discovery-team'
