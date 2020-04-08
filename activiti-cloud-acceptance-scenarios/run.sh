#!/usr/bin/env bash

export REALM=activiti
export SSO_PROTOCOL=${SSO_PROTOCOL:-http}
export SSO_HOST=${SSO_HOST:-activiti-keycloak.jx-staging.35.228.195.195.nip.io}
export GATEWAY_PROTOCOL=${GATEWAY_PROTOCOL:-http}
export GATEWAY_HOST=${GATEWAY_HOST:-activiti-cloud-gateway.jx-staging.35.228.195.195.nip.io}
export SSO_URL=${SSO_URL:-${SSO_PROTOCOL}://${SSO_HOST}/auth}
export GATEWAY_URL=${GATEWAY_URL:-${GATEWAY_PROTOCOL}://${GATEWAY_HOST}}
echo running tests on env:
echo REALM=${REALM}
echo SSO_URL=${SSO_URL}
echo GATEWAY_URL=${GATEWAY_URL}
mvn clean verify serenity:aggregate
