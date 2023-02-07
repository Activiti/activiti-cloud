#!/usr/bin/env bash

export REALM=${REALM:-activiti}
export DOMAIN=${DOMAIN:-activiti-community.envalfresco.com}
export NAMESPACE=${NAMESPACE:-default}
export GATEWAY_PROTOCOL=${GATEWAY_PROTOCOL:-http}
export GATEWAY_HOST=${GATEWAY_HOST:-gateway-$NAMESPACE.$DOMAIN}
export SSO_PROTOCOL=${SSO_PROTOCOL:-http}
export SSO_HOST=${SSO_HOST:-identity-$NAMESPACE.$DOMAIN}
export SSO_URL=${SSO_URL:-${SSO_PROTOCOL}://${SSO_HOST}/auth}
export GATEWAY_URL=${GATEWAY_URL:-${GATEWAY_PROTOCOL}://${GATEWAY_HOST}}
echo running tests on env:
echo "- REALM=${REALM}"
echo "- SSO_URL=${SSO_URL}"
echo "- GATEWAY_URL=${GATEWAY_URL}"
mvn clean verify serenity:aggregate

helm install feature-$(whoami) \
  activiti-cloud-helm-charts/activiti-cloud-full-example \
  --set global.gateway.domain=activiti-community.envalfresco.com \
  --namespace feature-$(whoami) --create-namespace
