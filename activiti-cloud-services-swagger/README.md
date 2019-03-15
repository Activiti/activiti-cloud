# Activiti Cloud Services Swagger

This module provides base springfox configuration for swagger auto-generated specification file. It provides two
swagger specification files: 

- default: available under `v2/api-docs` or `v2/api-docs?group=default`;
 provides specification for Alfresco MediaType format

- HAL: available under `v2/api-docs?group=hal`; provides specification for HAL format

## How to use it
- Add a Maven dependency to this project:

```xml
<dependency>
  <groupId>org.activiti.cloud.common</groupId>
  <artifactId>activiti-cloud-services-swagger</artifactId>
</dependency>
```
- Declare a bean the will select the apis to be scanned. I.e.:
```
@Bean
public Predicate<RequestHandler> apiSelector() {
    return RequestHandlerSelectors.basePackage("org.activiti.cloud.services")::apply;
}
```

*Note:* make sure the controllers are returning Spring objects only (`PagedResources<Resource<DomainObject>>`, 
`Resources<Resource<DomainObject>>`, `Resource<DomainObject>`); the mapping will not work if custom `*Resource` 
are used.