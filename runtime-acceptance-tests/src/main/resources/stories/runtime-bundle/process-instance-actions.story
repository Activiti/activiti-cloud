Meta:

Narrative:
As a user
I want to perform operations on process instances

Scenario: check all process definitions are present
Given the user is authenticated as testuser
When the user gets the process definitions
Then all the process definitions are present

Scenario: delete a process instance
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And the user deletes the process
Then the process instance is deleted

Scenario: resume a cancelled process instance
Given the user is authenticated as testuser
When the user starts an instance of the process called SIMPLE_PROCESS_INSTANCE
And the user suspends the process instance
Then the user is able to resume the process instance

Scenario: try activate a cancelled process instance
Given the user is authenticated as testuser
And any suspended process instance
When the user deletes the process
Then the process cannot be activated anymore

Scenario: show a process instance diagram
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And open the process diagram
Then the diagram is shown

Scenario: show diagram for a process instance without graphic info
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITHOUT_GRAPHIC_INFO
And open the process diagram
Then no diagram is shown

Scenario: complete a process instance that uses a connector
Given the user is authenticated as testuser
When the user starts a process with variables called CONNECTOR_PROCESS_INSTANCE
Then the status of the process is changed to completed
And a variable was created with name var1
And a variable was created with name test-json-variable-result
And a variable was created with name test-long-json-variable-result
And a variable was created with name test-int-variable-result
And a variable was created with name test-bool-variable-result

Scenario: check all process definitions are present as admin
Given the user is authenticated as hradmin
Then The user gets all the process definitions in admin endpoint

Scenario: retrieve process instances as an admin
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user can get process with variables instances in admin endpoint

Scenario: query process instances as an admin
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user can query process with variables instances in admin endpoints

Scenario: get events as an admin
Given the user is authenticated as hradmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
Then the user can get events for process with variables instances in admin endpoint

Scenario: check the presence of formKey field in task
Given the user is authenticated as testuser
Then the PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED definition has the formKey field with value startForm

Scenario: check the process is updated
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And the user updates the name of the process instance to new-process-name
Then the process instance is updated
And the process has the name new-process-name

Scenario: start a process instance with a name
Given the user is authenticated as testuser
When the user set a process instance name my_process_instance_name and starts the process SIMPLE_PROCESS_INSTANCE
Then verify the process instance name is my_process_instance_name

Scenario: admin delete a process instance
Given the user is authenticated as testadmin
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And the admin deletes the process
Then the process instance is deleted

Scenario: check sequence number and message id for events
Given the user is authenticated as testuser
When the user starts an instance of the process called SIMPLE_PROCESS_INSTANCE
Then the generated events have sequence number set
And the generated events have the same message id

