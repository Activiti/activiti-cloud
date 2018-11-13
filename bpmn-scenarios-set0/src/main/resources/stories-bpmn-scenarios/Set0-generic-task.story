Meta:

Narrative:
As a user
I want to perform an action
So that I can achieve a business goal

Scenario: start a process with generic BMPN task
Given the user is authenticated as testuser
When the user starts a process called Process with Generic BPMN Task
Then the process is completed
And the events are as expected for Process with Generic BPMN Task