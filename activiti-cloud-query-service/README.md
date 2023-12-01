# activiti-cloud-query-service

Activiti Cloud Query Service &amp; Spring Boot Starters.

Implements a query service that can be added to an application using starter.

Structure:

- model - Basic JPA entity model for querying.
- repo - Enables persistence for the core model.
- rest - Implementation for consuming event stream from runtime bundle and also provides REST endpoints. Uses \* spring data.

**Note:** in order to get this project working propertly in your IDE you'll need to build it a first time from the command line as it has some auto-generated classes.
