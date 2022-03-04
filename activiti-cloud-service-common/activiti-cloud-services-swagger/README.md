# Activiti Cloud Services Swagger

This module provides base springfox and springdoc configuration for swagger auto-generated specification file.

It provides two swagger specification files:

-  default: available under `v3/api-docs` or `v3/api-docs?group=default`;
   provides specification for Alfresco MediaType format

-  HAL: available under `v2/api-docs?group=hal`; provides specification for HAL format

*Note:* make sure the controllers are returning Spring objects only (`PagedModel<EntityModel<DomainObject>>`,
`CollectionModel<EntityModel<DomainObject>>`, `EntityModel<DomainObject>`); the mapping will not work if custom `*Model`
are used.

## How to use it
- Add a Maven dependency to this project:

```xml
<dependency>
  <groupId>org.activiti.cloud.common</groupId>
  <artifactId>activiti-cloud-services-swagger</artifactId>
</dependency>
```

###Springdoc
When adding this as dependency provide following properties
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

###Springfox (deprecated)
- Declare a bean the will select the apis to be scanned. I.e.:
```
@Bean
public Predicate<RequestHandler> apiSelector() {
    return RequestHandlerSelectors.basePackage("org.activiti.cloud.services");
}
```
