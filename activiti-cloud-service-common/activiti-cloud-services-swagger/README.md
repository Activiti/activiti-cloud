# Activiti Cloud Services Swagger

This module provides base springfox and springdoc configuration for swagger auto-generated specification file.

It provides swagger specification files:

-   for springfox available under `v3/api-docs` or `v3/api-docs?group=[groupName]`;
    provides specification for Alfresco MediaType format

-   for springdoc available under `v3/api-docs` or `v3/api-docs/[groupName]`;
    provides specification for Alfresco MediaType format

*Note:* make sure the controllers are returning Spring objects only (`PagedModel<EntityModel<DomainObject>>`,
`CollectionModel<EntityModel<DomainObject>>`, `EntityModel<DomainObject>`); the mapping will not work if custom `*Model`
are used.

## How to use it
-   Add a Maven dependency to this project:

```xml
<dependency>
  <groupId>org.activiti.cloud.common</groupId>
  <artifactId>activiti-cloud-services-swagger</artifactId>
</dependency>
```

### Springdoc
When adding this as dependency provide

#### for base OpenApi
the following properties
```
    springdoc.enabled=true
    springdoc.packages-to-scan=[base-package-to-scan]
    springdoc.api-docs.path=[path-to-custom-api-docs]
```
and a bean for OpenApi:
```
    @Bean
    public OpenAPI baseOpenApi(BaseOpenApiBuilder baseOpenApiBuilder) {
        return baseOpenApiBuilder.build("title", "service-url-prefix");
    }
```
#### for group OpenApi
the following property
```
    springdoc.enabled=true
```
and a bean for GroupedOpenApi:
```
    @Bean
    public GroupedOpenApi groupedOpenApi() {
        return GroupedOpenApi.builder()
            .group("group-name")
            .packagesToScan([base-package-to-scan])
            .build();
    }
```

### Springfox (deprecated)
-   Declare a bean that will select the apis to be scanned. I.e.:
```
@Bean
public Predicate<RequestHandler> apiSelector() {
    return RequestHandlerSelectors.basePackage("org.activiti.cloud.services");
}
```
