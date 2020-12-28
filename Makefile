RELEASE_VERSION := $(or $(shell cat VERSION), $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout))
ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR := .git/activiti-cloud-full-chart
ACTIVITI_CLOUD_FULL_EXAMPLE_DIR := $(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR)/charts/activiti-cloud-full-example
ACTIVITI_CLOUD_FULL_CHART_BRANCH := dependency-activiti-cloud-application-$(RELEASE_VERSION)

updatebot/push-version:
	updatebot push-version --kind maven \
		org.activiti.cloud:activiti-cloud-dependencies ${RELEASE_VERSION} \
		org.activiti.cloud:activiti-cloud-modeling-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-audit-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-api-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-parent ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-connectors-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-messages-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-modeling-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-notifications-graphql-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-query-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-runtime-bundle-dependencies ${ACTIVITI_CLOUD_VERSION} \
		org.activiti.cloud:activiti-cloud-service-common-dependencies ${ACTIVITI_CLOUD_VERSION} \
		--merge false

dependabot:
	curl --silent --show-error --fail -X POST -H "Content-Type: application/json" \
		-d "{\"name\":\"org.activiti.cloud:activiti-cloud-dependencies\", \"version\": \"$(RELEASE_VERSION)\", \"package-manager\": \"maven\"}" \
		-H "Authorization: Personal ${GITHUB_TOKEN}" \
		https://api.dependabot.com/release_notifications/private

install: release
	echo helm $(helm version --short)
	cd $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR) && \
		helm dep up && \
		helm upgrade ${PREVIEW_NAMESPACE} . \
			--install \
			--set global.gateway.http=false \
			--set global.gateway.domain=${GLOBAL_GATEWAY_DOMAIN} \
			--namespace ${PREVIEW_NAMESPACE} \
			--create-namespace \
			--wait

delete:
	helm delete ${PREVIEW_NAMESPACE} --namespace ${PREVIEW_NAMESPACE} || echo "try to remove helm chart"
	kubectl delete ns ${PREVIEW_NAMESPACE} || echo "try to remove namespace ${PREVIEW_NAMESPACE}"

clone-chart:
	rm -rf $(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR) && \
		git clone https://${GITHUB_TOKEN}@github.com/Activiti/activiti-cloud-full-chart.git $(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR) --depth 1

create-pr: update-chart
	cd $(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR) && \
		(git push -q origin :$(ACTIVITI_CLOUD_FULL_CHART_BRANCH) || true) && \
	  git checkout -q -b $(ACTIVITI_CLOUD_FULL_CHART_BRANCH) && \
		helm-docs && \
		git diff && \
		git commit -am "Update 'activiti-cloud-application' dependency to $(RELEASE_VERSION)" && \
		git push -qu origin $(ACTIVITI_CLOUD_FULL_CHART_BRANCH) && \
		gh pr create --fill --head $(ACTIVITI_CLOUD_FULL_CHART_BRANCH) --label updatebot ${GH_PR_CREATE_OPTS}

update-chart: clone-chart
	cd $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR) && \
		yq write --inplace Chart.yaml 'version' $(RELEASE_VERSION) && \
		env BACKEND_VERSION=$(RELEASE_VERSION) FRONTEND_VERSION=master make update-docker-images

release: update-chart
	echo "RELEASE_VERSION: $(RELEASE_VERSION)"
	cd $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR) && \
    helm dep up && \
    helm lint && \
    cat Chart.yaml && \
	  cat values.yaml && \
	  ls charts -la

docker/%:
	$(eval MODULE=$(word 2, $(subst /, ,$@)))

	mvn verify -B -pl $(MODULE) -am
	@echo "Building docker image for $(MODULE):$(RELEASE_VERSION)..."
	docker build -f $(MODULE)/Dockerfile -q -t docker.io/activiti/$(MODULE):$(RELEASE_VERSION) $(MODULE)
	docker push docker.io/activiti/$(MODULE):$(RELEASE_VERSION)

docker-delete/%:
	$(eval MODULE=$(word 2, $(subst /, ,$@)))

	@echo "Delete image from Docker Hub for $(MODULE):$(RELEASE_VERSION)..."
	curl --silent --show-error --fail -X DELETE -u "$DOCKER_REGISTRY_USERNAME:$DOCKER_REGISTRY_PASSWORD" \
		https://hub.docker.com/v2/repositories/activiti/$(MODULE)/tags/$(RELEASE_VERSION)

docker-delete-all: docker-delete/example-runtime-bundle docker-delete/activiti-cloud-query docker-delete/example-cloud-connector docker-delete/activiti-cloud-modeling

version:
	mvn versions:set -DprocessAllModules=true -DgenerateBackupPoms=false -DnewVersion=$(RELEASE_VERSION)

deploy:
	mvn clean deploy -DskipTests

tag:
	git add -u
	git commit -m "Release $(RELEASE_VERSION)" --allow-empty
	git tag -fa v$(RELEASE_VERSION) -m "Release version $(RELEASE_VERSION)" || travis_terminate 1;
	git push -f -q https://${GITHUB_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git v$(RELEASE_VERSION) || travis_terminate 1;

test/%:
	$(eval MODULE=$(word 2, $(subst /, ,$@)))

	cd activiti-cloud-acceptance-scenarios && \
		mvn -pl '$(MODULE)' -Droot.log.level=off verify

promote: version deploy tag updatebot/push-version dependabot create-pr
