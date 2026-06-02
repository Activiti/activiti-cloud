Meta:

Narrative:
As a user
I want to perform an operation on tasks
So that I can get results on a running process

Scenario: subprocess task is created when starting a parent process with call activities
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_CALL_ACTIVITIES
Then the task from SUB_PROCESS_INSTANCE_WITH_TASK is CREATED and it is called subprocess-task

Scenario: check the presence of formKey field in task
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
Then the task has the formKey field and correct processInstance fields

Scenario: tasks have their own copies of variables
Given the user is authenticated as testuser
When the user starts with variables for TWO_TASK_PROCESS with variables start1 and start2
And a task variable was created with name start1
And task variable start1 has value start1
And a task variable was created with name start2
And the user claims the task
And we update task variable start1 to start1modified
And task variable start1 has value start1modified
And the user completes the task
And another user is authenticated as hruser
And a task variable was created with name start1
And a task variable was created with name start2
Then task variable start1 has value start1

Scenario: check the task is updated
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And the user updates the updatable fields of the task
Then the task is updated
And the task has the updated fields

Scenario: check root tasks for the process TWO_TASK_PROCESS
Given the user is authenticated as testuser
When the user starts an instance of the process called TWO_TASK_PROCESS
Then the user will get only root tasks when quering for root tasks

Scenario: check the task has completion fields
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And the user completes the task
Then the task has the completion fields set

Scenario: check the task is updated by admin
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
And another user is authenticated as testadmin
And the admin updates the updatable fields of the task
And another user is authenticated as testuser
Then the task is updated
And the task has the updated fields

Scenario: save a task
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And the user claims the task
And the user saves the task with variable status equal to approved
Then task variable status has value approved

Scenario: complete saved task
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And the user claims the task
And the user saves the task with variable status equal to approved
And the user completes the task
Then the status of the process is changed to completed
And query process instance variable status has value approved

Scenario: complete saved task with outcome
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES
And the user claims the task
And the user saves the task with variable comments equal to lgtm
And the user completes the task with variable outcome set to approved
Then the status of the process is changed to completed
And query process instance variable comments has value lgtm
And query process instance variable outcome has value approved

Scenario: should not remove candidate groups for a task with group candidates
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES
And the status of the task is CREATED
And the task contains candidate groups hr,testgroup in Query
And the user claims the task
And the user completes the task
Then the status of the process and the task is changed to completed
And the status of the task is COMPLETED in Audit and Query
And the task contains candidate groups hr,testgroup in Query

Scenario: current assignee of a task can reassign it to a candidate user
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES
And the task contains candidate users hruser in Query
And the user claims the task
And the user assign the task to hruser
And another user is authenticated as hruser
Then the assignee is hruser

Scenario: current assignee of a task cannot reassign it to a user that is not a candidate
Given the user is authenticated as testuser
When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED
When the task does not contain candidate user hruser in Query
Then the user cannot assign the task to hruser
