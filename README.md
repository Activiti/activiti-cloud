# Activiti Cloud Query (JPA)

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status Travis](https://travis-ci.org/Activiti/activiti-cloud-query.svg?branch=master)](https://travis-ci.org/Activiti/activiti-cloud-query)
[![Coverage Status](http://img.shields.io/codecov/c/github/Activiti/activiti-cloud-query/master.svg?maxAge=86400)](https://codecov.io/gh/Activiti/activiti-cloud-query)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud-query/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud-query)](https://cla-assistant.io/Activiti/activiti-cloud-query)
[![Docker Build Status](https://img.shields.io/docker/build/activiti/activiti-cloud-query.svg)](https://hub.docker.com/r/activiti/activiti-cloud-query/)
[![Known Vulnerabilities](https://snyk.io/test/github/Activiti/activiti-cloud-query/badge.svg)](https://snyk.io/test/github/Activiti/activiti-cloud-query)

Activiti Cloud Query Service JPA Implementation. This service uses a Relational Database to store events emitted by Runtime Bundles in a performant way for reading and querying. This is our reference implementation, but we encourange you to modify and adapt to suit your domain specific needs.

As all our services, this module was build using the [activiti-cloud-starter-query](https://github.com/activiti/activiti-cloud-query-service) module, that you can use to create your own version of this service as with any other Spring Boot Starter.  

For more information about his module and the starters you can take a look at our [Activiti & Activiti Cloud GitBook](https://activiti.gitbooks.io/activiti-7-developers-guide/content/components/activiti-cloud-app/QueryService.html)

[Docker Image](https://hub.docker.com/r/activiti/activiti-cloud-query/)

## Building & Running this Service
You can build this service from source using Git & Maven or you can just run our Docker Image. 

### Spring Boot: 
> git clone https://github.com/Activiti/activiti-cloud-query.git
> cd activiti-cloud-query/
> mvn clean install spring-boot:run

### Docker: 
> docker run -p 8182:8182 -d --name activiti-cloud-query activiti/activiti-cloud-query:latest

## Environemnt Variables
```
server.port=${ACT_QUERY_PORT:8182}
spring.application.name=${ACT_QUERY_APP_NAME:query}
spring.cloud.stream.bindings.producer.destination=${ACT_QUERY_PRODUCER_DEST:engineEvents}
spring.cloud.stream.bindings.producer.contentType=${ACT_QUERY_PRODUCER_CONTENT_TYPE:application/json}
spring.cloud.stream.bindings.queryConsumer.destination=${ACT_QUERY_CONSUMER_DEST:engineEvents}
spring.cloud.stream.bindings.queryConsumer.group=${ACT_QUERY_CONSUMER_GROUP:query}
spring.cloud.stream.bindings.queryConsumer.contentType=${ACT_QUERY_CONSUMER_CONTENT_TYPE:application/json}
spring.jackson.serialization.fail-on-unwrapped-type-identifiers=${ACT_QUERY_JACKSON_FAIL_ON_UNWRAPPED_IDS:false}
keycloak.auth-server-url=${ACT_KEYCLOAK_URL:http://activiti-cloud-sso-idm:8180/auth}
keycloak.realm=${ACT_KEYCLOAK_REALM:springboot}
keycloak.resource=${ACT_KEYCLOAK_RESOURCE:activiti}
keycloak.public-client=${ACT_KEYCLOAK_CLIENT:true}
keycloak.ssl-required=${ACT_KEYCLOAK_SSL_REQUIRED:none}
keycloak.security-constraints[0].authRoles[0]=${ACT_KEYCLOAK_ROLES:user}
keycloak.security-constraints[0].securityCollections[0].patterns[0]=${ACT_KEYCLOAK_PATTERNS:/*}
keycloak.principal-attribute=${ACT_KEYCLOAK_PRINCIPAL_ATTRIBUTE:preferred-username}
activiti.keycloak.admin-client-app=${ACT_KEYCLOAK_CLIENT_APP:admin-cli}
activiti.keycloak.client-user=${ACT_KEYCLOAK_CLIENT_USER:client}
activiti.keycloak.client-password=${ACT_KEYCLOAK_CLIENT_PASSWORD:client}
spring.rabbitmq.host=${ACT_RABBITMQ_HOST:rabbitmq}
eureka.client.serviceUrl.defaultZone=${ACT_EUREKA_URL:http://activiti-cloud-registry:8761/eureka/}
eureka.instance.hostname=${ACT_QUERY_HOST:activiti-cloud-query}
eureka.client.enabled=${ACT_QUERY_EUREKA_CLIENT_ENABLED:true}
```