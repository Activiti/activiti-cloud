# activiti-cloud-query-service 

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status Travis](https://travis-ci.org/Activiti/activiti-cloud-query-service.svg?branch=master)](https://travis-ci.org/Activiti/activiti-cloud-query-service)
[![Coverage Status](http://img.shields.io/codecov/c/github/Activiti/activiti-cloud-query-service/master.svg?maxAge=86400)](https://codecov.io/gh/Activiti/activiti-cloud-query-service)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5ee4e6ceacda459a9bffb12e5fb4574d)](https://www.codacy.com/app/Activiti/activiti-cloud-query-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Activiti/activiti-cloud-query-service&amp;utm_campaign=Badge_Grade)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud-query-service/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud-query-service)](https://cla-assistant.io/Activiti/activiti-cloud-query-service)
[![Known Vulnerabilities](https://snyk.io/test/github/Activiti/activiti-cloud-query-service/badge.svg)](https://snyk.io/test/github/Activiti/activiti-cloud-query-service)

Activiti Cloud Query Service &amp; Spring Boot Starters.

Implements a query service that can be added to an application using starter. Supports either REST or GraphQL.

Structure:
* graphiql - A UI client for testing graphQL requests.
* graphql - This makes the graphql endpoint available - it has a dependency to model.
* graphqlws - Configures websockes for graphql and sets up schema with event for subscribing to - needed for subscribing to notifications.
* model - Basic JPA entity model for querying.
* notifications - Transforms event stream for consumption by GraphQL - can be run as separate component (see module README).
* repo - Enables persistence for the core model.
* rest - Implementation for consuming event stream from runtime bundle and also provides REST endpoints (but not related to graphql). Uses * spring data.
* stomp - Adds websocket dependency and /ws/stomp endpoint. Needed for subscribing to notifications.