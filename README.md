'mvn clean verify -Dprofile=docker' will run the test against services started with docker-compose.
'mvn clean verify -Dprofile=kubernetes' will run the test against services started with kubernetes.

For custom values for gateway and sso hosts:

> export GATEWAY_HOST=<custom-gateway-host>:<custom-gateway-port>
> export SSO_HOST=<custom-sso-host>:<custom-sso-port>
> mvn clean verify
