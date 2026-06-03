# Acceptance Tests Scenarios for Activiti Cloud

This repo now includes a set of modules which contains different acceptances tests. This acceptance tests rely on having an environment to run against.

In order to point to an environment you can export the following _ENVIRONMENT VARIABLES_

```
> export GATEWAY_HOST=<custom-gateway-host>:<custom-gateway-port>
> export SSO_HOST=<custom-sso-host>:<custom-sso-port>
> export REALM=activiti
```

to use _https_ rather than _http_:

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

In order to run remaining Serenity scenarios (legacy — prefer Playwright in `activiti-cloud-acceptance-tests-playwright/`):

```
cd activiti-cloud-acceptance-scenarios
mvn clean verify
```

Migrated areas (security policies, multi-runtime signal, process-instance-actions, task-actions wave 1) were removed from this module; see [DEPRECATED.md](DEPRECATED.md).
