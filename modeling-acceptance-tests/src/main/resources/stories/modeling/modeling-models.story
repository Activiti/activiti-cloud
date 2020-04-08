Meta:

Narrative:
As a user
I want to manage process and form models

Scenario: create a process model
Given the user is authenticated as modeler
And an existing project 'mars-team'
When the user opens the project 'mars-team'
And creates the process model 'recruiting-crew-for-mars'
Then the process model 'recruiting-crew-for-mars' is created
And the version of the process model 'recruiting-crew-for-mars' is 0.0.1

Scenario: create the second version of an existing version of a process model
Given the user is authenticated as modeler
And an project 'mars-team' with process model 'recruiting-crew' version 0.0.1
When the user opens the project 'mars-team'
And opens the process model 'recruiting-crew'
And edits and saves the model
Then the model is saved with the version 0.0.2

Scenario: create a process model with process variables
Given the user is authenticated as modeler
When the user opens the existing project 'mars-team'
And creates the process model recruiting-team with process variables 'age, gender'
And opens the process model 'recruiting-team'
Then it contains process variables 'age, gender'

Scenario: change process variables of a process model
Given the user is authenticated as modeler
When the user opens the existing project 'mars-team'
And creates the process model recruiting-team with process variables 'age, gender'
And opens the process model 'recruiting-team'
And removes the process variable 'age'
And adds the process variable 'experience'
And saves the model
Then the model is saved with the process variables 'gender, experience'

Scenario: validate process model file
Given the user is authenticated as modeler
And an project 'mission-europa' with process model landing-rover and process variables 'age, gender'
When opens the process model 'landing-rover'
Then the model is valid

Scenario: validate invalid process model file
Given the user is authenticated as modeler
And an existing project 'nasa-team'
When the user opens the project 'nasa-team'
And creates the process model spacex-launch with process variables 'age, gender'
And opens the process model 'spacex-launch'
Then properties not valid find in extensions
