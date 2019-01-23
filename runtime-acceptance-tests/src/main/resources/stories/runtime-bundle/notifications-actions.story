Meta:

Narrative:
As a user
I want to perform operations on process instances

Scenario: complete a process instance that uses a connector
Given the user is authenticated as testadmin
When the user starts a process with notifications called CONNECTOR_PROCESS_INSTANCE
Then the status of the process is completed
Then notifications are received
