Meta:

Narrative:
As a user
I want to perform operations on process instance variables

Scenario: admin update process instance variables
Given the user is authenticated as hradmin
When the admin starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2
And the admin update the instance variables start1 with value value1 and start2 with value value2
Then the list of errors messages is empty
And variable start1 has value value1 and start2 has value value2

Scenario: admin set process instance variables
Given the user is authenticated as hradmin
When the admin starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2
And the user set the instance variable dummy1 with value dummyValue1
And the user set the instance variable dummy2 with value dummyValue2
Then variable dummy1 has value dummyValue1 and dummy2 has value dummyValue2

Scenario: admin delete process instance variables
Given the user is authenticated as hradmin
When the admin starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2
And the user set the instance variable dummy1 with value dummyValue1
And the user set the instance variable dummy2 with value dummyValue2
And the admin delete the instance variable dummy1
Then the process variable dummy1 is deleted
And the process variable dummy2 is created
