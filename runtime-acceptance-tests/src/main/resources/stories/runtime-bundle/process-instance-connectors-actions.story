Meta:
Narrative:
As a user
I want to perform operations os processes containing connectors

Scenario: Start a process containing cloud connector
Given the user is authenticated as testuser
And the user provides a variable named movieToRank with value The Lord of The Rings
When the user starts an instance of process called RankMovieId with the provided variables
Then the process instance has a variable named movieToRank with value The Lord of The Rings
And the process instance has a variable named movieDesc with value The Lord of the Rings is an epic high fantasy novel written by English author and scholar J. R. R. Tolkien
And the process instance has a task named Add Rating