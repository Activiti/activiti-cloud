# Activiti Cloud Query (JPA)

Activiti Cloud Query Service JPA Implementation. This service uses a Relational Database to store events emitted by Runtime Bundles in a performant way for reading and querying. This is our reference implementation, but we encourange you to modify and adapt to suit your domain specific needs.

As all our services, this module was build using our starters:

-   [activiti-cloud-starter-query](https://github.com/Activiti/activiti-cloud/tree/develop/activiti-cloud-query-service/activiti-cloud-starter-query)
-   [activiti-cloud-starter-audit](https://github.com/Activiti/activiti-cloud/tree/develop/activiti-cloud-audit-service/activiti-audit-starter-query)
-   [activiti-cloud-starter-notification-graphql](https://github.com/Activiti/activiti-cloud/tree/develop/activiti-cloud-notification-graphql-service/activiti-cloud-starter-notification-graphql)

You can use them to create your own version of this service as with any other Spring Boot Starter.
Alternative, you can also split in multiple services, for instance there are two examples that include only Activiti Cloud Query Notifications and Activiti Cloud Query Audit:

-   [activiti-cloud-audit](https://github.com/activiti/activiti-cloud-audit)
-   [activiti-cloud-notification-graphql](https://github.com/activiti/activiti-cloud-notification-graphql)

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

## Environment Variables
```
server.port=${ACT_QUERY_PORT:8182}
spring.application.name=${ACT_QUERY_APP_NAME:query}
spring.cloud.stream.bindings.producer.destination=${ACT_QUERY_PRODUCER_DEST:engineEvents}
spring.cloud.stream.bindings.producer.contentType=${ACT_QUERY_PRODUCER_CONTENT_TYPE:application/json}
spring.cloud.stream.bindings.queryConsumer.destination=${ACT_QUERY_CONSUMER_DEST:engineEvents}
spring.cloud.stream.bindings.queryConsumer.group=${ACT_QUERY_CONSUMER_GROUP:query}
spring.cloud.stream.bindings.queryConsumer.contentType=${ACT_QUERY_CONSUMER_CONTENT_TYPE:application/json}
spring.cloud.stream.bindings.auditConsumer.destination=${ACT_AUDIT_CONSUMER_DEST:engineEvents}
spring.cloud.stream.bindings.auditConsumer.group=${ACT_AUDIT_CONSUMER_GROUP:audit}
spring.cloud.stream.bindings.auditConsumer.contentType=${ACT_AUDIT_CONSUMER_CONTENT_TYPE:application/json}
spring.jackson.serialization.fail-on-unwrapped-type-identifiers=${ACT_QUERY_JACKSON_FAIL_ON_UNWRAPPED_IDS:false}
authorizations.security-constraints[0].authRoles[0]=${ACT_KEYCLOAK_ROLES:user}
authorizations.security-constraints[0].securityCollections[0].patterns[0]=${ACT_KEYCLOAK_PATTERNS:/*}
spring.rabbitmq.host=${ACT_RABBITMQ_HOST:rabbitmq}
eureka.client.serviceUrl.defaultZone=${ACT_EUREKA_URL:http://activiti-cloud-registry:8761/eureka/}
eureka.instance.hostname=${ACT_QUERY_HOST:activiti-cloud-query}
eureka.client.enabled=${ACT_QUERY_EUREKA_CLIENT_ENABLED:true}
```
