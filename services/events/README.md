# Notifications Module 

<p>This module consumes events made available by an Activiti runtime bundle (by default though the engineEvents destination). It transforms these events to a flattened format and passes on to queues for consumption by clients.</p>

<p>For clients to subscribe to the queues they need to go through the GraphQL query module and register their subscription, either by calling the /ws/graphql endpoint or by calling /graphql and being automatically fed there. The event schema needs to be registered with GraphQL (which it is through the -ws module).</p>

<p>The notifications gateway is a separable component that can be run in another container or pod.</p>

<p>In order to separate:</p>

- Remove the @EnableActivitiNotificationsGateway from the QueryApplication (otherwise gateway will be embedded in it)</br>
- Create a new application called NotificationsApplication and add the @EnableActivitiNotificationsGateway annotation (it will need the activiti-cloud-services-query-notifications dependency)</br>
- In its application.properties it will need the notificationsConsumer and notificationsGateway configuration that is currently in QueryApplication</br>
- Create a docker image for the new notifications gateway application and add to deployment descriptors as desired.</br>