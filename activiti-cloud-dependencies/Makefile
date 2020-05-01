ACTIVITI_CLOUD_CONNECTORS_VERSION := $(shell grep -oPm1 "(?<=<example-cloud-connector.version>)[^<]+" "dependencies-tests/pom.xml")
ACTIVITI_CLOUD_QUERY_VERSION := $(shell grep -oPm1 "(?<=<activiti-cloud-query.version>)[^<]+" "dependencies-tests/pom.xml")
ACTIVITI_CLOUD_RB_VERSION := $(shell grep -oPm1 "(?<=<example-runtime-bundle.version>)[^<]+" "dependencies-tests/pom.xml")
ACTIVITI_CLOUD_MODELING_VERSION := $(shell grep -oPm1 "(?<=<activiti-cloud-modeling.version>)[^<]+" "dependencies-tests/pom.xml")

ACTIVITI_CLOUD_VERSION_NUMBER := $(shell grep -oPm1 "(?<=<activiti-cloud-build.version>)[^<]+" "pom.xml")

#PREVIEW_NAMESPACE := $(shell echo $(PREVIEW_NAMESPACE)|  tr '[:upper:]' '[:lower:]')


ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION := 7.1.273
ACTIVITI_CLOUD_COMMON_HELM_CHART := 1.1.21

GITHUB_CHARTS_BRANCH := $(or $(GITHUB_CHARTS_BRANCH),gh-pages)

ACTIVITI_CLOUD_SERVICES_VERSIONS := org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies  $(ACTIVITI_CLOUD_VERSION_NUMBER) org.activiti.cloud.connector:activiti-cloud-connectors-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) \
    org.activiti.cloud.query:activiti-cloud-query-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) org.activiti.cloud.notifications.graphql:activiti-cloud-notifications-graphql-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER)  \
    org.activiti.cloud.audit:activiti-cloud-audit-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) org.activiti.cloud.modeling:activiti-cloud-modeling-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER) \
    org.activiti.cloud.messages:activiti-cloud-messages-dependencies $(ACTIVITI_CLOUD_VERSION_NUMBER)

ACTIVITI_CLOUD_FULL_CHART_VERSIONS := runtime-bundle $(VERSION) activiti-cloud-connector $(VERSION) \
    activiti-cloud-query $(VERSION)  \
    activiti-cloud-modeling $(VERSION)

$(eval HELM_ACTIVITI_VERSION = $(shell cat VERSION |rev|sed 's/\./-/'|rev))

GITHUB_CHARTS_BRANCH := "gh-pages"


ACTIVITI_CLOUD_VERSION := $(shell cat VERSION)
get-acc-scenarious-version:
	@echo $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION)

activiti-cloud-acceptance-scenarios:
	rm -rf activiti-cloud-acceptance-scenarios && \
	git clone https://github.com/Activiti/activiti-cloud-acceptance-scenarios.git && \
	cd activiti-cloud-acceptance-scenarios && \
	git fetch --all --tags --prune && \
	git checkout tags/v$(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION) -b $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION) && \
	mvn clean install -DskipTests

modeling-acceptance-tests:
	cd activiti-cloud-acceptance-scenarios && \
	mvn -pl 'modeling-acceptance-tests' clean verify

runtime-acceptance-tests:
	cd activiti-cloud-acceptance-scenarios && \
	mvn -pl 'runtime-acceptance-tests' clean verify

update-ea:
	#$(eval ACTIVITI_CLOUD_VERSION = $(shell cat VERSION))
	@echo "ACTIVITI_CLOUD_VERSION=<$(ACTIVITI_CLOUD_VERSION)>"

	$(eval ID = $(shell echo ${ACTIVITI_CLOUD_VERSION}|tr -dc '[:alnum:]\n\r'))
	@echo ID=${ID}

	rm -rf alfresco-process-parent||echo removing alfresco-process-parent
	git clone https://oauth2:${GITLAB_TOKEN}@git.alfresco.com/process-services/alfresco-process-parent.git
	@echo "Clone for alfresco-process-parent done"

	cd alfresco-process-parent && \
	  git checkout develop && \
	  git checkout -b update-cloud-to-$(ACTIVITI_CLOUD_VERSION) && \
	  mvn versions:set-property -Dproperty=activiti-cloud.version -DnewVersion=$(ACTIVITI_CLOUD_VERSION) && \
	  git diff --word-diff && \
	  git commit -a -m "AAE-0 update ACTIVITI_CLOUD_VERSION to ${ACTIVITI_CLOUD_VERSION}" && \
	  git push  --set-upstream origin update-cloud-to-${ACTIVITI_CLOUD_VERSION}
	@cd alfresco-process-parent && curl --request POST --header "PRIVATE-TOKEN: $(GITLAB_TOKEN)" --header "Content-Type: application/json" -d '{"id": ${ID} ,"source_branch": "update-cloud-to-${ACTIVITI_CLOUD_VERSION}" ,"target_branch":"develop","title":"community propagation ACTIVITI_CLOUD_VERSION to ${ACTIVITI_CLOUD_VERSION}"}' https://git.alfresco.com/api/v4/projects/1031/merge_requests

updatebot/push-version:
	updatebot push-version --kind maven org.activiti.cloud.dependencies:activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_SERVICES_VERSIONS)   --merge false
	updatebot push-version --kind helm activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_FULL_CHART_VERSIONS)
	updatebot push-version --kind make ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION)

updatebot/push-version-dry:
	#updatebot --dry push-version --kind maven org.activiti.cloud.dependencies:activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_SERVICES_VERSIONS)   --merge false
	updatebot --dry push-version --kind helm activiti-cloud-dependencies $(ACTIVITI_CLOUD_VERSION) $(ACTIVITI_CLOUD_FULL_CHART_VERSIONS)
	#updatebot --dry push-version --kind make ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION)

replace-release-full-chart-names:
	echo HELM_ACTIVITI_VERSION = $(HELM_ACTIVITI_VERSION)
	echo APP_ACTIVITI_VERSION = $(APP_ACTIVITI_VERSION)
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
	 sed -i -e "s/appVersion: .*/appVersion: $(HELM_ACTIVITI_VERSION)/" Chart.yaml && \
	 sed -i -e "s/version: .*/version: $(HELM_ACTIVITI_VERSION)/" Chart.yaml && \
	 sed -i -e "s/#tag: .*/tag: $(APP_ACTIVITI_VERSION)/" values.yaml

pull-docker-images:
	docker pull activiti/activiti-cloud-query:$(ACTIVITI_CLOUD_QUERY_VERSION)
	docker pull activiti/example-runtime-bundle:$(ACTIVITI_CLOUD_RB_VERSION)
	docker pull activiti/example-cloud-connector:$(ACTIVITI_CLOUD_CONNECTORS_VERSION)
	docker pull activiti/activiti-cloud-modeling:$(ACTIVITI_CLOUD_MODELING_VERSION)

retag-docker-images: pull-docker-images
	docker image tag activiti/activiti-cloud-query:$(ACTIVITI_CLOUD_QUERY_VERSION) activiti/activiti-cloud-query:$(ACTIVITI_CLOUD_VERSION)
	docker image tag activiti/example-runtime-bundle:$(ACTIVITI_CLOUD_RB_VERSION) activiti/example-runtime-bundle:$(ACTIVITI_CLOUD_VERSION)
	docker image tag activiti/example-cloud-connector:$(ACTIVITI_CLOUD_CONNECTORS_VERSION) activiti/example-cloud-connector:$(ACTIVITI_CLOUD_VERSION)
	docker image tag activiti/activiti-cloud-modeling:$(ACTIVITI_CLOUD_MODELING_VERSION) activiti/activiti-cloud-modeling:$(ACTIVITI_CLOUD_VERSION)

push-docker-images:
	docker push activiti/activiti-cloud-query:$(ACTIVITI_CLOUD_VERSION)
	docker push activiti/example-runtime-bundle:$(ACTIVITI_CLOUD_VERSION)
	docker push activiti/example-cloud-connector:$(ACTIVITI_CLOUD_VERSION)
	docker push activiti/activiti-cloud-modeling:$(ACTIVITI_CLOUD_VERSION)

github:
	$(eval GITHUB_CHARTS_DIR := $(shell basename $(GITHUB_CHARTS_REPO) .git))
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
	[[ -d $(GITHUB_CHARTS_DIR) ]] ||git clone -b "$(GITHUB_CHARTS_BRANCH)" "$(GITHUB_CHARTS_REPO)" $(GITHUB_CHARTS_DIR) &&\
	cp "activiti-cloud-full-example-$(HELM_ACTIVITI_VERSION).tgz" $(GITHUB_CHARTS_DIR)

	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/$(GITHUB_CHARTS_DIR) && \
	   helm repo index . && \
	   git add . && \
	   git status && \
	   git commit -m "fix:(version) release activiti-cloud-full-example-$(HELM_ACTIVITI_VERSION).tgz" && \
	   git pull && \
	   git push --force origin "$(GITHUB_CHARTS_BRANCH)"
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
     rm -rf $(GITHUB_CHARTS_DIR)

tag:
	#sed -i -e "s/version:.*/version: $(HELM_ACTIVITI_VERSION)/" Chart.yaml
	#sed -i -e "s/tag: .*/tag: $(RELEASE_VERSION)/" values.yaml
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
	git add Chart.yaml values.yaml requirements.yaml && \
	git commit -m "release $(HELM_ACTIVITI_VERSION)" --allow-empty && \
	git tag -fa v$(HELM_ACTIVITI_VERSION) -m "Release version $(HELM_ACTIVITI_VERSION)" && \
	git push origin v$(HELM_ACTIVITI_VERSION)


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

delete:
	helm delete --purge ${PREVIEW_NAMESPACE} || echo "try to remove helm chart"


RELEASE_GREP_EXPR := '^[Rr]elease'

git-rev-list:
	$(eval REV = $(shell git rev-list --tags --max-count=1 --grep $(RELEASE_GREP_EXPR)))
	$(eval PREVIOUS_REV = $(shell git rev-list --tags --max-count=1 --skip=1 --grep $(RELEASE_GREP_EXPR)))
	$(eval REV_TAG = $(shell git describe ${PREVIOUS_REV}))
	$(eval PREVIOUS_REV_TAG = $(shell git describe ${REV}))
	@echo Found commits between $(PREVIOUS_REV_TAG) and $(REV_TAG) tags:
	git rev-list $(PREVIOUS_REV)..$(REV) --first-parent --pretty
