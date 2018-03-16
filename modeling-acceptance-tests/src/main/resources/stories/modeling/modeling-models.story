Meta:

Narrative:
As a user
I want to manage process and form models

Scenario: create a project
Given any authenticated user
And an existing group 'Recruiting'
When the user opens the group 'Recruiting'
And creates a project 'Mars Team'
Then the project 'Mars Team' is created

Scenario: create a process model in a project
Given any authenticated user
And an existing project 'Mars Team'
When the user opens the project 'Mars Team'
And creates the process model 'Recruiting Crew for Mars'
Then the process model 'Recruiting Crew for Mars' is created
And the version of the process model 'Recruiting Crew for Mars' is 0.0.1

Scenario: create the second version of an existing version of a process model
Given any authenticated user
And an existing process model 'Recruiting Crew for Mars' with version 0.0.1
When the user opens the process model 'Recruiting Crew for Mars'
And edits and saves the model
Then the model is saved with the version 0.0.2