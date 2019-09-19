Meta:

Narrative:
As a user
I want to perform operations on process instance having message events

Scenario: deliver messages via process runtime Rest Api
Given session timeout of 5 seconds
And the user is authenticated as hruser
And generated unique sessionVariable called businessId
When the user sends a start message named startMessage with businessKey value of businessId
Then MESSAGE_RECEIVED event is emitted for the message 'startMessage'
And MESSAGE_WAITING event is emitted for the message 'boundaryMessage'
And the user sends a message named boundaryMessage with correlationKey value of businessId
And MESSAGE_RECEIVED event is emitted for the message 'boundaryMessage'
And MESSAGE_WAITING event is emitted for the message 'catchMessage'
And the user sends a message named catchMessage with correlationKey value of businessId
And MESSAGE_RECEIVED event is emitted for the message 'catchMessage'
And MESSAGE_SENT event is emitted for the message 'endMessage'
And the process with message events is completed
