Meta:

Narrative:
As a user
I want to perform operations on process instance having inclusive gateway

Scenario: complete a process instance with inclusive gateway
Given the user is authenticated as hruser
When the user starts a process with inclusive gateway PROCESS_INSTANCE_WITH_INCLUSIVE_GATEWAY and set input variable value to 1
Then the task is created Start Process
When the user claims and completes the task Start Process
Then events are emitted for the inclusive gateway inclusiveGateway
Then the user will see 2 tasks
And the task is created Send e-mail
And the task is created Check account
When the user claims and completes the task Send e-mail
Then the user will see 1 tasks
And the task is created Check account
When the user claims and completes the task Check account
Then events are emitted for the inclusive gateway inclusiveGatewayEnd
Then the process with inclusive gateway is completed
