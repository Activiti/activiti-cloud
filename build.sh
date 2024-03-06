#!/usr/bin/env bash
for SERVICE in activiti-cloud-query example-cloud-connector example-runtime-bundle
do
  docker build -f $SERVICE/Dockerfile -q -t docker.io/activiti/$SERVICE:${TAG:-latest} $SERVICE
done
