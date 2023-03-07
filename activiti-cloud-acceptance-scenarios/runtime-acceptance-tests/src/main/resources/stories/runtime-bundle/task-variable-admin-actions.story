Meta:

Narrative:
As an admin
I want to perform an actions on task varaibles

Scenario: create task variable as admin
Given the user is authenticated as hradmin
And the user creates a standalone task
When the user creates, using admin endpoint, a task variable named title with value Mr.
Then the user is able to retrieve, using the admin endpoint, a variable named title with value Mr. as part of task variables

Scenario: update task variable as admin
Given the user is authenticated as hradmin
And the user creates a standalone task
And the user creates, using admin endpoint, a task variable named title with value Mr.
When the user updates, using admin endpoint, the task variable named title with value Dr.
Then the user is able to retrieve, using the admin endpoint, a variable named title with value Dr. as part of task variables
