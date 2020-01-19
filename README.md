# activiti-cloud-build

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status Travis](https://travis-ci.com/Activiti/activiti-cloud-build.svg?branch=master)](https://travis-ci.com/Activiti/activiti-cloud-build)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud-build/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud-build)](https://cla-assistant.io/Activiti/activiti-cloud-build)
[![Known Vulnerabilities](https://snyk.io/test/github/Activiti/activiti-cloud-build/badge.svg)](https://snyk.io/test/github/Activiti/activiti-cloud-build)
[![security status](https://www.meterian.com/badge/gh/Activiti/activiti-cloud-build/security)](https://www.meterian.com/report/gh/Activiti/activiti-cloud-build)
[![stability status](https://www.meterian.com/badge/gh/Activiti/activiti-cloud-build/stability)](https://www.meterian.com/report/gh/Activiti/activiti-cloud-build)

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
        <version>${activiti-cloud.version}</version>
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
