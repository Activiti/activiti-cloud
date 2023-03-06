# Acceptance Tests Scenarios for Activiti Cloud

This repo now includes a set of modules which contains different acceptances tests. This acceptance tests rely on having an environment to run against.

In order to point to an environment you can export the following *ENVIRONMENT VARIABLES*

```
> export GATEWAY_HOST=<custom-gateway-host>:<custom-gateway-port>
> export SSO_HOST=<custom-sso-host>:<custom-sso-port>
> export REALM=activiti
```

to use *https* rather than *http*:
```
> export GATEWAY_PROTOCOL=https
> export SSO_PROTOCOL=https
```

or specify the full URL:
```
> export GATEWAY_URL=<custom-gateway-url>
> export SSO_URL=<custom-sso-url>
```

You can use our HELM charts hosted here: [Activiti Cloud HELM Charts](https://github.com/Activiti/activiti-cloud-charts/tree/master/activiti-cloud-full-example) to create these environments
with all the services that are tested by these acceptance tests.


In order to run these acceptance tests you can run:

```
> mvn clean install -DskipTests && mvn -pl '!apps-acceptance-tests,!multiple-runtime-acceptance-tests,!security-policies-acceptance-tests' clean verify
```

This will ignore the following modules: apps-acceptance-tests,multiple-runtime-acceptance-tests,security-policies-acceptance-tests and run all the others.
This is extremely useful to control which tests run depending on your environment configurations and why you are trying to test.
