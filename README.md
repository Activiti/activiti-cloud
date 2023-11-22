# activiti-cloud

[![Join Us in Gitter](https://badges.gitter.im/Activiti/Activiti7.svg)](https://gitter.im/Activiti/Activiti7?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![CI](https://github.com/Activiti/activiti-cloud/actions/workflows/main.yml/badge.svg)](https://github.com/Activiti/activiti-cloud/actions/workflows/main.yml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/05862b3db7344b24b4509d266df77c3a)](https://www.codacy.com/gh/Activiti/activiti-cloud?utm_source=github.com&utm_medium=referral&utm_content=Activiti/activiti-cloud&utm_campaign=Badge_Grade)
[![ASL 2.0](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/Activiti/activiti-cloud/blob/master/LICENSE.txt)
[![CLA](https://cla-assistant.io/readme/badge/Activiti/activiti-cloud)](https://cla-assistant.io/Activiti/activiti-cloud)
[![security status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud/security)](https://www.meterian.io/report/gh/Activiti/activiti-cloud)
[![stability status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud/stability)](https://www.meterian.io/report/gh/Activiti/activiti-cloud)
[![licensing status](https://www.meterian.io/badge/gh/Activiti/activiti-cloud/licensing)](https://www.meterian.io/report/gh/Activiti/activiti-cloud)

Activiti Cloud libraries and Spring Boot starters.

## CI/CD

Running on GH Actions.

For Dependabot PRs to be validated by CI, the label "CI" should be added to the PR.

Requires the following secrets to be set:

| Name                         | Description                        |
| ---------------------------- | ---------------------------------- |
| BOT_GITHUB_TOKEN             | Token to launch other builds on GH |
| BOT_GITHUB_USERNAME          | Username to issue propagation PRs  |
| DOCKERHUB_USERNAME           | Docker Hub repository username     |
| DOCKERHUB_PASSWORD           | Docker Hub repository password     |
| NEXUS_USERNAME               | Internal Maven repository username |
| NEXUS_PASSWORD               | Internal Maven repository password |
| RANCHER2_URL                 | Rancher URL for tests              |
| RANCHER2_ACCESS_KEY          | Rancher access key for tests       |
| RANCHER2_SECRET_KEY          | Rancher secret key for tests       |
| SLACK_NOTIFICATION_BOT_TOKEN | Token to notify slack on failure   |

## Formatting

The local `.editorconfig` file is leveraged for automated formatting.

See documentation at [pre-commit](https://github.com/Alfresco/alfresco-build-tools/tree/master/docs#pre-commit).

To run all hooks locally:

```sh
pre-commit run -a
```


export MESSAGING_BROKER=rabbitmq
export MESSAGING_PARTITIONED=partitioned
export MESSAGING_DESTINATIONS=default-destinations
export PREVIEW_NAME=pr-test

# fill VERSION

export SSO_PROTOCOL=http
export GATEWAY_PROTOCOL=http
export GLOBAL_GATEWAY_DOMAIN=localhost

export GATEWAY_HOST=gateway-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN
export SSO_HOST=identity-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN


kind create cluster --name "chart-testing" --config ~/apa/alfresco-build-tools/.github/actions/setup-kind/kind.yml

kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s

mvnnotest
make docker-all
make kind-load-docker-all
make install