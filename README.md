# activiti-cloud-query-service 

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status Travis](https://travis-ci.org/Activiti/activiti-cloud-query-service.svg?branch=master)](https://travis-ci.org/Activiti/activiti-cloud-query-service)
[![Coverage Status](http://img.shields.io/codecov/c/github/Activiti/activiti-cloud-query-service/master.svg?maxAge=86400)](https://codecov.io/gh/Activiti/activiti-cloud-query-service)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5ee4e6ceacda459a9bffb12e5fb4574d)](https://www.codacy.com/app/Activiti/activiti-cloud-query-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Activiti/activiti-cloud-query-service&amp;utm_campaign=Badge_Grade)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud-query-service/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud-query-service)](https://cla-assistant.io/Activiti/activiti-cloud-query-service)
[![Known Vulnerabilities](https://snyk.io/test/github/Activiti/activiti-cloud-query-service/badge.svg)](https://snyk.io/test/github/Activiti/activiti-cloud-query-service)
[![security status](https://www.meterian.com/badge/gh/Activiti/activiti-cloud-query-service/security)](https://www.meterian.com/report/gh/Activiti/activiti-cloud-query-service)
[![stability status](https://www.meterian.com/badge/gh/Activiti/activiti-cloud-query-service/stability)](https://www.meterian.com/report/gh/Activiti/activiti-cloud-query-service)

Activiti Cloud Query Service &amp; Spring Boot Starters.

Implements a query service that can be added to an application using starter. 

Structure:
* model - Basic JPA entity model for querying.
* repo - Enables persistence for the core model.
* rest - Implementation for consuming event stream from runtime bundle and also provides REST endpoints. Uses * spring data.