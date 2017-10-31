# activiti-cloud-build
Activiti Cloud Parent and BOM (Bill of Materials)

## activiti-cloud-parent
This pom.xml file handle all 3rd Party dependencies that are shared by all the starters. Spring Boot & Spring Cloud versions are defined here. 

## activiti-cloud-dependencies
This BOM (Bill Of Materials) allow you to easily add the following section to your maven pom.xml file:

```
<dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.activiti.cloud</groupId>
        <artifactId>activiti-cloud-dependencies</artifactId>
        <version>${version.activiti.cloud}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ```
To automatically manage all cloud artifact versions and make sure that you don't mix different versions from different releases of these components. 
Then you can include any starter or service by just adding the GroupId and ArtifactId

For example:
```
<dependency>
    <groupId>org.activiti.cloud</groupId>
    <artifactId>activiti-cloud-starter-runtime-bundle</artifactId>
</dependency>
```

Versions are going to be handled by maven. 

## Spring Version Alignment

We are using spring boot and spring cloud. These versions need to be aligned. See:

http://projects.spring.io/spring-cloud/

And tags at:

https://github.com/spring-cloud/spring-cloud-release