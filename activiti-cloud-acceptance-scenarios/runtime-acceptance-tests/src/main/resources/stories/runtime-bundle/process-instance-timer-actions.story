Meta:

Narrative:
As a user
I want to perform operations on process instance having timer events

Scenario: check a process instance with intermediate timer event
Given the user is authenticated as hruser
When the user starts a process with timer events called INTERMEDIATE_TIMER_EVENT_PROCESS
Then TIMER_SCHEDULED events are emitted for the timer 'timer' and timeout 5 seconds
And TIMER_EXECUTED events are emitted for the timer 'timer' and timeout 10 seconds
And the process with timer events is completed

Scenario: check a process instance with start timer event
Given the user is authenticated as testadmin
Then the admin query returns 2 processes called START_TIMER_EVENT_PROCESS with timeout 60 seconds
And timer events are emitted for processes called START_TIMER_EVENT_PROCESS

Scenario: check a process instance with boundary timer event
Given the user is authenticated as hruser
When the user starts a process with timer events called BOUNDARY_TIMER_EVENT_PROCESS
Then TIMER_SCHEDULED boundary events are emitted for the timer 'timer' and timeout 5 seconds
And TIMER_EXECUTED events are emitted for the timer 'timer' and timeout 10 seconds
And the process with timer events is completed
