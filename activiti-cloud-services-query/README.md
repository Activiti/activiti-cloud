# Query Service

This service provides querying capabilities. It is distinct from the run-time API which is used to perform actions on engine items. This implementation contains REST endpoints.

## REST API Approach

The service provides query endpoints with paging and sorting. So an example query might be e.g. /query/processinstances?status=RUNNING&page=0&size=10&sort=lastModified,desc

The approach is based upon https://github.com/spring-projects/spring-data-examples/tree/master/web/querydsl . It supports querying for nested objects and nested collections by specifying the path with '.'

If the Q* classes aren't present in the /target/generated-sources directory then run mvn generate-sources from this project directory

## Database Support

The implementation is using spring data in as agnostic a way as available so that alternative databases could be used.

The project has a split structure so that the activiti-cloud-services-query-repo module can be excluded and replaced with an alternative implementation (e.g. (https://www.mkyong.com/spring-boot/spring-boot-spring-data-elasticsearch-example/) at least for the REST API.