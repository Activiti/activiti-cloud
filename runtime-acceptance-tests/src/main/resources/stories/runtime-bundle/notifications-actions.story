Meta:

Narrative:
As a user
I want to perform operations on process instances with subscriptions to receive notifications

Scenario: complete a process instance that uses a connector with subscription
Given the user is authenticated as testadmin
When the user starts a process CONNECTOR_PROCESS_INSTANCE with PROCESS_STARTED and PROCESS_COMPLETED events subscriptions
Then the status of the process is completed
Then PROCESS_STARTED and PROCESS_COMPLETED notifications are received

Scenario: complete a process instance that sends a signal with subscription 
Given the user is authenticated as testadmin
When the user starts a process SIGNAL_THROW_PROCESS_INSTANCE with SIGNAL_RECEIVED subscription
Then the status of the process is completed
And SIGNAL_RECEIVED notification with theStart signal event is received

Scenario: complete a process instance with intermediate timer subscription 
Given the user is authenticated as testadmin
When the user starts a process INTERMEDIATE_TIMER_EVENT_PROCESS with TIMER subscriptions
Then the status of the process is completed
And TIMER notifications are received

Scenario: complete a process instance with boundary timer subscription 
Given the user is authenticated as testadmin
When the user starts a process BOUNDARY_TIMER_EVENT_PROCESS with TIMER subscriptions
Then the status of the process is completed
And TIMER notifications are received
