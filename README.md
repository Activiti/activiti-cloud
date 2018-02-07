# activiti-cloud-query-service 

<p>Implements a query service that can be added to an application using starter. Supports either REST or GraphQL.</p>

<p>Structure:</p>

graphiql - A UI client for testing graphQL requests.<br/>
graphql - This makes the graphql endpoint available - it has a dependency to model.<br/>
graphqlws - Configures websockes for graphql and sets up schema with event for subscribing to - needed for subscribing to notifications.</br>
model - Basic JPA entity model for querying.<br/>
notifications - Transforms event stream for consumption by GraphQL - can be run as separate component (see module README).<br/>
repo - Enables persistence for the core model.<br/>
rest - Implementation for consuming event stream from runtime bundle and also provides REST endpoints (but not related to graphql). Uses spring data.<br/>
stomp - Adds websocket dependency and /ws/stomp endpoint. Needed for subscribing to notifications.</br>

[![Join the chat at https://gitter.im/Activiti/Activiti7](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<p>
  <a title='Build Status Travis' href="https://travis-ci.org/Activiti/activiti-cloud-query-service">
    <img src='https://travis-ci.org/Activiti/activiti-cloud-query-service.svg?branch=master'  alt='Travis Status' />
  </a>
  <a href='https://codecov.io/gh/Activiti/activiti-cloud-query-service'>
    <img src='http://img.shields.io/codecov/c/github/Activiti/activiti-cloud-query-service/master.svg?maxAge=86400' alt='Coverage Status' />
  </a>
  <a href='https://www.codacy.com/app/Activiti/activiti-cloud-query-service?utm_source=github.com&utm_medium=referral&utm_content=Activiti/activiti-cloud-query-service&utm_campaign=badger'>
      <img src='https://api.codacy.com/project/badge/Grade/5ee4e6ceacda459a9bffb12e5fb4574d' alt='codacy' />
  </a>
  <a href='https://github.com/Activiti/activiti-cloud-query-service/blob/master/LICENSE.txt'>
       <img src='https://img.shields.io/hexpm/l/plug.svg' alt='license' />
  </a>
  <a href="https://cla-assistant.io/Activiti/activiti-cloud-query-service"><img src="https://cla-assistant.io/readme/badge/Activiti/activiti-cloud-query-service" alt="CLA assistant" /></a>
</p>
Activiti Cloud Query Service &amp; Spring Boot Starters
