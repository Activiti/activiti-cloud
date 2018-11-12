Meta:

Narrative:
As a user
I want to perform an action
So that I can achieve a business goal

Scenario: start a basic process
Given the user is authenticated as testuser
When A - the user starts a Process Information
Then the process is completed
And the events are as expected for Process Information

Scenario: check events in basic process with variables
Given the user is authenticated as testuser
When C - the user starts a Process Information with variables
Then the events are as expected for Process Information with variables

Scenario: check the propagation of information of the basic process
Given the user is authenticated as testuser
When B - the user starts a Process Information with name processInstanceName and businessKey businessKey
Then the process instance information is correctly propagated



Scenario: start a process with generic BMPN task
Given the user is authenticated as testuser
When A - the user starts a Process with Generic BPMN Task
Then the process is completed
And the events are as expected for Process with Generic BPMN Task