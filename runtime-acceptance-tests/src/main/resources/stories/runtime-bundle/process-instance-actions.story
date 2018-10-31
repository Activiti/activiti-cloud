Meta:

Narrative:
As a user
I want to perform operations on process instances

Scenario: delete a process instance
Given the user is authenticated as testuser
When the user starts a process with variables
And the user deletes the process
Then the process instance is deleted


Scenario: try activate a cancelled process instance
Given the user is authenticated as testuser
And any suspended process instance
When the user deletes the process
Then the process cannot be activated anymore

Scenario: show a process instance diagram
Given the user is authenticated as testuser
When the user starts a process with variables
And open the process diagram
Then the diagram is shown

Scenario: show diagram for a process instance without graphic info
Given the user is authenticated as testuser
When the user starts a process without graphic info
And open the process diagram
Then no diagram is shown

Scenario: complete a process instance that uses a connector
Given the user is authenticated as testuser
When the user starts a connector process
Then the status of the process is changed to completed
And a variable was created with name var1

Scenario: retrieve process instances as an admin
Given the user is authenticated as hradmin
When the user starts a process with variables
Then the user can get process with variables instances in admin endpoint

Scenario: query process instances as an admin
Given the user is authenticated as hradmin
When the user starts a process with variables
Then the user can query process with variables instances in admin endpoints

Scenario: get events as an admin
Given the user is authenticated as hradmin
When the user starts a process with variables
Then the user can get events for process with variables instances in admin endpoint

