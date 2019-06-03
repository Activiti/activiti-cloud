Meta:

Narrative:
As a user
I want to perform operations on process instance having subProcess

Scenario: complete a process instance with a subProcess
Given the user is authenticated as hruser
When the user starts a process with a supProcess called PROCESS_INSTANCE_WITH_EMBEDDED_SUB_PROCESS
And the user claims the task
And the user completes the task
Then subProcess events are emitted
And the process is completed
