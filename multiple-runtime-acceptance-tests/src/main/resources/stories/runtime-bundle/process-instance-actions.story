Meta:

Narrative:
As a user
I want to perform operations on process instances

Scenario: signal between multiple runtime bundle services
Given the user is authenticated as testuser
When the user starts signal catch process on primary runtime and starts signal throw process on secondary runtime
Then a signal was received and the signal catch process was completed.