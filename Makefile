CURRENT=$(shell pwd)
NAME := $(or $(APP_NAME),$(shell basename $(CURRENT)))
OS := $(shell uname)

RELEASE_VERSION := $(or $(shell cat VERSION), $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout))
GROUP_ID := $(shell mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
ARTIFACT_ID := $(shell mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
RELEASE_ARTIFACT := $(GROUP_ID):$(ARTIFACT_ID)

ACTIVITI_CLOUD_FULL_CHART_VERSIONS := runtime-bundle $(VERSION) activiti-cloud-connector $(VERSION) \
    activiti-cloud-query $(VERSION)  \
    activiti-cloud-modeling $(VERSION)
charts := "activiti-cloud-query/charts/activiti-cloud-query" "example-runtime-bundle/charts/runtime-bundle" "example-cloud-connector/charts/activiti-cloud-connector" "activiti-cloud-modeling/charts/activiti-cloud-modeling/"

updatebot/push:
	@echo doing updatebot push $(RELEASE_VERSION)
	updatebot push --ref $(RELEASE_VERSION)

updatebot/push-version-dry:
	echo $(VESION)
	#updatebot --dry push-version --kind maven org.activiti.cloud.dependencies:activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_SERVICES_VERSIONS)   --merge false
	updatebot --dry push-version --kind helm activiti-cloud-dependencies $(RELEASE_VERSION) $(ACTIVITI_CLOUD_FULL_CHART_VERSIONS)
	#updatebot --dry push-version --kind make ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION)


updatebot/push-version:
	@echo Resolving push versions for artifacts........
	$(eval ACTIVITI_CLOUD_VERSION=$(shell mvn help:evaluate -Dexpression=activiti-cloud-mono-aggregator.version -q -DforceStdout))

	@echo Doing updatebot push-version.....
	@echo updatebot push-version --dry --kind maven \
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


prepare-helm-chart:
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
		rm -rf requirements.lock && \
		rm -rf charts && \
		rm -rf *.tgz && \
        	helm init --client-only && \
        	helm repo add activiti-cloud-helm-charts https://activiti.github.io/activiti-cloud-helm-charts/ && \
        	helm repo add alfresco https://kubernetes-charts.alfresco.com/stable	&& \
        	helm repo add alfresco-incubator https://kubernetes-charts.alfresco.com/incubator && \
        	helm dependency build && \
        	helm lint && \
		helm package .

run-helm-chart:
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
            	helm upgrade ${PREVIEW_NAMESPACE} . \
            		--install \
            		--set global.gateway.domain=${GLOBAL_GATEWAY_DOMAIN} \
            		--namespace ${PREVIEW_NAMESPACE} \
            		--wait
								
update-version-in-example-charts:
	@for chart in $(charts) ; do \
		cd $$chart ; \
		sed -i -e "s/version:.*/version: $$VERSION/" Chart.yaml; \
		sed -i -e "s/tag: .*/tag: $$VERSION/" values.yaml ;\
		cd - ; \
	done 
create-helm-charts-release-and-upload:
	@for chart in $(charts) ; do \
		cd $$chart ; \
		make version; \
		make build; \
		make release; \
		make github; \
		cd - ; \
	done 
