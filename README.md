'mvn clean verify -Dprofile=docker' will run the test against services started with docker-compose.
'mvn clean verify -Dprofile=kubernetes' will run the test against services started with kubernetes.

If 'profile' variable is missing, the docker profile is assumed.