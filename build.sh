#!/usr/bin/env bash
cd activiti-cloud-examples
for SERVICE in activiti-cloud-query example-cloud-connector example-runtime-bundle activiti-cloud-modeling
do
  docker build -f $SERVICE/Dockerfile -q -t docker.io/activiti/$SERVICE:${TAG:-latest} $SERVICE
done
