CURRENT=$(shell pwd)
NAME := $(or $(APP_NAME),$(shell basename $(CURRENT)))
OS := $(shell uname)

RELEASE_VERSION := $(or $(shell cat VERSION), $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout))
GROUP_ID := $(shell mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
ARTIFACT_ID := $(shell mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
RELEASE_ARTIFACT := $(GROUP_ID):$(ARTIFACT_ID)

updatebot/push:
	@echo doing updatebot push $(RELEASE_VERSION)
	updatebot push --ref $(RELEASE_VERSION)


updatebot/push-version:
	@echo Resolving push versions for artifacts........
	$(eval ACTIVITI_CLOUD_VERSION=$(shell mvn help:evaluate -Dexpression=activiti-cloud-mono-aggregator.version -q -DforceStdout))

	@echo Doing updatebot push-version.....
	@echo updatebot push-version --kind maven \
		org.activiti.cloud.modeling:activiti-cloud-modeling-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.audit:activiti-cloud-audit-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.api:activiti-cloud-api-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.build:activiti-cloud-parent $(RELEASE_VERSION) \
		org.activiti.cloud.build:activiti-cloud-dependencies-parent $(RELEASE_VERSION)\
		org.activiti.cloud.connector:activiti-cloud-connectors-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.messages:activiti-cloud-messages-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.modeling:activiti-cloud-modeling-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.notifications.graphql:activiti-cloud-notifications-graphql-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.query:activiti-cloud-query-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies $(RELEASE_VERSION) \
		org.activiti.cloud.common:activiti-cloud-service-common-dependencies $(RELEASE_VERSION)

updatebot/update:
	@echo doing updatebot update $(RELEASE_VERSION)
	updatebot update

updatebot/update-loop:
	@echo doing updatebot update-loop $(RELEASE_VERSION)
	updatebot update-loop --poll-time-ms 60000

