ACTIVITI_CLOUD_VERSION_NUMBER := $(shell grep -oPm1 "(?<=<activiti-cloud-build.version>)[^<]+" "pom.xml")

GITHUB_CHARTS_BRANCH := $(or $(GITHUB_CHARTS_BRANCH),gh-pages)

$(eval HELM_ACTIVITI_VERSION = $(or $(HELM_ACTIVITI_VERSION),$(shell cat VERSION |rev|sed 's/\./-/'|rev)))

ACTIVITI_CLOUD_SERVICES_VERSIONS := org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies  $(ACTIVITI_CLOUD_VERSION_NUMBER) org.activiti.cloud.connector:activiti-cloud-connectors-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) \
    org.activiti.cloud.query:activiti-cloud-query-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) org.activiti.cloud.notifications.graphql:activiti-cloud-notifications-graphql-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER)  \
    org.activiti.cloud.audit:activiti-cloud-audit-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) org.activiti.cloud.modeling:activiti-cloud-modeling-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) \
    org.activiti.cloud.messages:activiti-cloud-messages-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER)

ACTIVITI_CLOUD_FULL_CHART_VERSIONS := runtime-bundle $(VERSION) activiti-cloud-connector $(VERSION) \
    activiti-cloud-query $(VERSION)  \
    activiti-cloud-modeling $(VERSION)

GITHUB_CHARTS_BRANCH := "gh-pages"

ACTIVITI_CLOUD_VERSION := $(shell cat VERSION)

modeling-acceptance-tests:
	cd activiti-cloud-acceptance-scenarios && \
	mvn -pl 'modeling-acceptance-tests' clean verify

runtime-acceptance-tests:
	cd activiti-cloud-acceptance-scenarios && \
	mvn -pl 'runtime-acceptance-tests' clean verify

updatebot/push-version:
	updatebot push-version --kind maven org.activiti.cloud.dependencies:activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_SERVICES_VERSIONS)   --merge false
	updatebot push-version --kind helm activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_FULL_CHART_VERSIONS)
	updatebot push-version --kind make ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION)

updatebot/push-version-dry:
	rm -rf  .updatebot-repos
	updatebot --dry push-version --kind helm activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_FULL_CHART_VERSIONS)
	
prepare-helm-chart:
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
	  cat requirements.yaml && \
		rm -rf requirements.lock && \
		rm -rf charts && \
		rm -rf *.tgz && \
        	helm init --client-only && \
        	helm repo add activiti-cloud-helm-charts https://activiti.github.io/activiti-cloud-helm-charts/ && \
        	helm repo add alfresco https://kubernetes-charts.alfresco.com/stable	&& \
        	helm repo add alfresco-incubator https://kubernetes-charts.alfresco.com/incubator && \
        	helm dep up && \
        	helm lint

run-helm-chart: delete
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
            	helm upgrade ${PREVIEW_NAMESPACE} . \
            		--install \
            		--set global.gateway.domain=${GLOBAL_GATEWAY_DOMAIN} \
            		--namespace ${PREVIEW_NAMESPACE} 
delete:
	helm delete --purge ${PREVIEW_NAMESPACE} || echo "try to remove helm chart"
	kubectl delete ns ${PREVIEW_NAMESPACE} || echo "try to remove namespace ${PREVIEW_NAMESPACE}"


RELEASE_GREP_EXPR := '^[Rr]elease'

git-rev-list:
	$(eval REV = $(shell git rev-list --tags --max-count=1 --grep $(RELEASE_GREP_EXPR)))
	$(eval PREVIOUS_REV = $(shell git rev-list --tags --max-count=1 --skip=1 --grep $(RELEASE_GREP_EXPR)))
	$(eval REV_TAG = $(shell git describe ${PREVIOUS_REV}))
	$(eval PREVIOUS_REV_TAG = $(shell git describe ${REV}))
	@echo Found commits between $(PREVIOUS_REV_TAG) and $(REV_TAG) tags:
	git rev-list $(PREVIOUS_REV)..$(REV) --first-parent --pretty
