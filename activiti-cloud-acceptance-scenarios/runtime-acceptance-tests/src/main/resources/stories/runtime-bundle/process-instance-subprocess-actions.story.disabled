Meta:

Narrative:
As a user
I want to perform operations on process instance having subProcess

Scenario: complete a process instance with a subProcess
Given the user is authenticated as hruser
When the user starts a process with tasks and a subProcess called PROCESS_INSTANCE_WITH_EMBEDDED_SUB_PROCESS
And the user claims the task declared in the subprocess
And the user completes the task declared in the subprocess
Then subProcess events are emitted
And the process with embedded subprocess is completed

Scenario: check variable mapping for a subprocess
Given the user is authenticated as hruser
When the user starts a process with a subProcess called PARENT_PROCESS
Then the parent process instance has a variable named name with value inName
And the subprocess has been created
And a subprocess variable subprocess_input_var1 is created with value inName
When the user claims and completes the subprocess task my-task-call-activity
Then the parent process instance has a variable named name with value outValue
