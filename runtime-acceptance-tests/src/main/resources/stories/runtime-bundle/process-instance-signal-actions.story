Meta:

Narrative:
As a user
I want to perform operations on process instances with throw catch signal

Scenario: process instances with throw, catch, boundary and start signal events
Given the user is authenticated as hruser
Then check number of processes with processDefinitionKey SignalStartEventProcess
When the user starts a process with intermediate catch signal
And the user starts a process with a boundary signal
Then the task is created with a name Boundary container
When the user starts a process with intermediate throw signal
Then the process throwing a signal is completed
And the process catching a signal is completed
And check number of processes with processDefinitionKey SignalStartEventProcess increased
And the SIGNAL_RECEIVED event was catched up by intermediateCatchEvent process
And the task is created with a name Boundary target
And the SIGNAL_RECEIVED event was catched up by boundary signal process
When another user is authenticated as testadmin
And the admin deletes boundary signal process
Then boundary signal process is deleted
