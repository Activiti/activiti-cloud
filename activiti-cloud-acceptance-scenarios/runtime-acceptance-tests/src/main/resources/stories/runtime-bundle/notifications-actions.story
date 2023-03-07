Meta:

Narrative:
As a user
I want to perform operations on process instances with subscriptions to receive event notifications

Scenario: complete a process instance that uses a connector with subscription to PROCESS event notifications
Given the user is authenticated as testadmin
And notifications: generated random value for session variable called businessKey
And notifications: session variable called process with value set to CONNECTOR_PROCESS_INSTANCE
When notifications: the user subscribes to PROCESS_STARTED,PROCESS_COMPLETED notifications
And notifications: the user starts a process CONNECTOR_PROCESS_INSTANCE
Then notifications: verify process instance started response
And notifications: the payload with PROCESS_STARTED notifications is expected
And notifications: the payload with PROCESS_COMPLETED notifications is expected
And notifications: verify the status of the process is completed
And notifications: the user completes the subscription
And notifications: verify all expected notifications are received

Scenario: complete a process instance that sends a signal with subscription to SIGNAL event notifications

Given the user is authenticated as testadmin
And notifications: session variable called businessKey with value set to *
And notifications: session variable called process with value set to SIGNAL_START_EVENT_PROCESS
When notifications: the user subscribes to SIGNAL_RECEIVED notifications
And notifications: the user starts a process SIGNAL_THROW_PROCESS_INSTANCE
Then notifications: verify process instance started response
And notifications: the payload with SIGNAL_RECEIVED notifications is expected with process definition key value SignalStartEventProcess
And notifications: verify the status of the process is completed
And notifications: the user completes the subscription
And notifications: verify all expected notifications are received

Scenario: complete a process instance with intermediate timer subscription to TIMER event notifications

Given the user is authenticated as testadmin
And notifications: session variable called businessKey with value set to businessKey
And notifications: session variable called process with value set to INTERMEDIATE_TIMER_EVENT_PROCESS
When notifications: the user subscribes to TIMER_SCHEDULED,TIMER_FIRED,TIMER_EXECUTED notifications
And notifications: the user starts a process INTERMEDIATE_TIMER_EVENT_PROCESS
Then notifications: verify process instance started response
And notifications: the payload with TIMER_SCHEDULED notifications is expected
And notifications: the payload with TIMER_FIRED,TIMER_EXECUTED notifications is expected
And notifications: verify the status of the process is completed
And notifications: the user completes the subscription
And notifications: verify all expected notifications are received

Scenario: complete a process instance with boundary timer subscription to TIMER event notifications

Given the user is authenticated as testadmin
And notifications: session variable called businessKey with value set to businessKey
And notifications: session variable called process with value set to BOUNDARY_TIMER_EVENT_PROCESS
When notifications: the user subscribes to TIMER_SCHEDULED,TIMER_FIRED,TIMER_EXECUTED notifications
And notifications: the user starts a process BOUNDARY_TIMER_EVENT_PROCESS
Then notifications: verify process instance started response
And notifications: the payload with TIMER_SCHEDULED notifications is expected
And notifications: the payload with TIMER_FIRED,TIMER_EXECUTED notifications is expected
And notifications: verify the status of the process is completed
And notifications: the user completes the subscription
And notifications: verify all expected notifications are received

Scenario: complete a process instance by messages with subscriptions to MESSAGE event notifications

Given the user is authenticated as testadmin
And notifications: generated random value for session variable called businessId
When notifications: the user subscribes to MESSAGE_RECEIVED,MESSAGE_WAITING,MESSAGE_SENT notifications with businessKey value from session variable called businessId
And notifications: the user sends a start message named startMessage with businessKey value from session variable called businessId
Then notifications: verify process instance started response
And notifications: the payload with MESSAGE_RECEIVED,MESSAGE_WAITING notifications is expected
And notifications: the user sends a message named boundaryMessage with correlationKey value of session variable called businessId
And notifications: the payload with MESSAGE_RECEIVED,MESSAGE_WAITING notifications is expected
And notifications: the user sends a message named catchMessage with correlationKey value of session variable called businessId
And notifications: the payload with MESSAGE_RECEIVED,MESSAGE_SENT notifications is expected
And notifications: verify the status of the process is completed
And notifications: the user completes the subscription
And notifications: verify all expected notifications are received
