Meta:

Narrative:
As a user
I want to perform operations on process instance having message events

Scenario: deliver messages via process runtime Rest Api
Given messages: session timeout of 5 seconds
And the user is authenticated as hruser
And messages: generated unique sessionVariable called businessId
When messages: the user sends a start message named startMessage with businessKey value of businessId session variable
Then messages: MESSAGE_RECEIVED event is emitted for the message 'startMessage'
And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'
And messages: the user sends a message named boundaryMessage with correlationKey value of businessId session variable
And messages: MESSAGE_RECEIVED event is emitted for the message 'boundaryMessage'
And messages: MESSAGE_WAITING event is emitted for the message 'catchMessage'
And messages: the user sends a message named catchMessage with correlationKey value of businessId session variable
And messages: MESSAGE_RECEIVED event is emitted for the message 'catchMessage'
And messages: MESSAGE_SENT event is emitted for the message 'endMessage'
And messages: the process with message events is completed
