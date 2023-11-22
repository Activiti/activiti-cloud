RELEASE_VERSION := $(or $(shell cat VERSION), $(shell python -c "from xml.etree.ElementTree import parse; print(parse(open('pom.xml')).find('{http://maven.apache.org/POM/4.0.0}version').text)"))
ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR := .git/activiti-cloud-full-chart
ACTIVITI_CLOUD_FULL_EXAMPLE_DIR := $(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR)/charts/activiti-cloud-full-example
ACTIVITI_CLOUD_FULL_CHART_BRANCH := dependency-activiti-cloud-application-$(RELEASE_VERSION)
ACTIVITI_CLOUD_FULL_CHART_RELEASE_BRANCH := $(or $(ACTIVITI_CLOUD_FULL_CHART_RELEASE_BRANCH),develop)

install: release
	echo helm $(helm version --short)
	test $(MESSAGING_BROKER) || exit 1
	test $(MESSAGING_PARTITIONED) || exit 1
	test $(MESSAGING_DESTINATIONS) || exit 1

	yq e -i '(.* | select(has("image")) | .image |select (has("pullPolicy"))).pullPolicy = "IfNotPresent"' $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml
	yq e -i '(.* | select(has("liquibase")) | .liquibase.image).pullPolicy = "IfNotPresent"' $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml

	yq e -i '.activiti-cloud-query.ingress.subPaths = ["/query/","/audit/","/notifications/"]' $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml

	yq e -i '.global.keycloak.url = "http://${PREVIEW_NAME}-k-http/auth"' $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml
	item=`echo "\n- name: KEYCLOAK_HOSTNAME\n  value: ${PREVIEW_NAME}-k-http\n"` yq e -i '.activiti-cloud-identity.extraEnv += strenv(item)' $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml

	# yq e -i '.global.keycloak.url = "http://${PREVIEW_NAME}-keycloak-http/auth"' $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml
	# cat $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR)/values.yaml

	cd $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR) && \
		helm dep up && \
		helm upgrade ${PREVIEW_NAME} . \
			--install \
			--set global.application.name=default-app \
			--set global.keycloak.clientSecret=$(shell uuidgen) \
			--set global.gateway.domain=${GLOBAL_GATEWAY_DOMAIN} \
			--values $(MESSAGING_BROKER)-values.yaml \
			--values $(MESSAGING_PARTITIONED)-values.yaml \
			--values $(MESSAGING_DESTINATIONS)-values.yaml \
			--namespace ${PREVIEW_NAME} \
			--create-namespace \
			--atomic \
			--wait \
			--timeout 8m

clone-chart:
	rm -rf $(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR) && \
		git clone https://${GITHUB_TOKEN}@github.com/Activiti/activiti-cloud-full-chart.git \
			--branch $(ACTIVITI_CLOUD_FULL_CHART_RELEASE_BRANCH) \
			$(ACTIVITI_CLOUD_FULL_CHART_CHECKOUT_DIR) \
			--depth 1

update-chart: clone-chart
	cd $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR) && \
		env VERSION=$(RELEASE_VERSION) make version && \
		env BACKEND_VERSION=$(RELEASE_VERSION) make update-docker-images

release: update-chart
	echo "RELEASE_VERSION: $(RELEASE_VERSION)"
	cd $(ACTIVITI_CLOUD_FULL_EXAMPLE_DIR) && \
	helm dep up && \
	helm lint

docker-all: docker/example-runtime-bundle \
	docker/activiti-cloud-query \
	docker/example-cloud-connector \
	docker/activiti-cloud-modeling \
	docker/activiti-cloud-identity-adapter

docker/%:
	$(eval MODULE=$(word 1, $(subst docker/, ,$@)))
	@echo "Building docker image for $(MODULE):$(RELEASE_VERSION)..."
	cd activiti-cloud-examples && docker build -f $(MODULE)/Dockerfile -q -t activiti/$(MODULE):$(RELEASE_VERSION) $(MODULE)

kind-load-docker-all: kind-load-docker/example-runtime-bundle \
	kind-load-docker/activiti-cloud-query \
	kind-load-docker/example-cloud-connector \
	kind-load-docker/activiti-cloud-modeling \
	kind-load-docker/activiti-cloud-identity-adapter

kind-load-docker/%:
	$(eval MODULE=$(word 1, $(subst kind-load-docker/, ,$@)))
	kind load docker-image activiti/$(MODULE):$(RELEASE_VERSION) --name chart-testing

version:
	mvn ${MAVEN_CLI_OPTS} versions:set -DprocessAllModules=true -DgenerateBackupPoms=false -DnewVersion=$(RELEASE_VERSION)

test/%:
	$(eval MODULE=$(word 2, $(subst /, ,$@)))
	mvn ${MAVEN_CLI_OPTS} -pl activiti-cloud-acceptance-scenarios/$(MODULE) -Droot.log.level=off verify -am

#	mvn ${MAVEN_CLI_OPTS} -f activiti-cloud-acceptance-scenarios/$(MODULE)/pom.xml -Droot.log.level=off verify -am

### cleanup methods, to be deleted with AAE-TODO

delete:
	helm uninstall ${PREVIEW_NAME} --namespace ${PREVIEW_NAME} || echo "try to remove helm chart"
	kubectl delete ns ${PREVIEW_NAME} || echo "try to remove namespace ${PREVIEW_NAME}"

# follow instructions at https://github.com/docker/hub-feedback/issues/496#issuecomment-277562292
docker-delete/%:
	$(eval MODULE=$(word 2, $(subst /, ,$@)))

	@echo "Retrieve token"
	$(eval TOKEN=$(shell curl --silent --show-error --fail \
		-X POST \
		-H "Content-Type: application/json" \
		-H "Accept: application/json" \
		-d '{"username":"${DOCKERHUB_USERNAME}","password":"${DOCKERHUB_PASSWORD}"}' \
		https://hub.docker.com/v2/users/login/ | jq ".token"))
	$(eval URL=https://hub.docker.com/v2/repositories/activiti/$(MODULE)/tags/$(RELEASE_VERSION))
	@echo "Delete image from Docker Hub at $(URL)"
	$(eval HTTP_CODE=$(shell curl --silent --show-error --fail \
		--write-out "%{http_code}" \
		-X DELETE \
		-H "Authorization: JWT $(TOKEN)" \
		$(URL)))
	$(eval e=0)
	@if [ $(HTTP_CODE) -ne 200 ]; then \
		e=1; \
	fi
	@if [ $(HTTP_CODE) -eq 404 ]; then \
		echo "::warning title=Image not found::Image not found: $(MODULE):$(RELEASE_VERSION)"; \
		e=0; \
	fi
	@echo "Delete token"
	@curl --silent --show-error --fail \
		-X POST \
		-H "Accept: application/json" \
		-H "Authorization: JWT $(TOKEN)" \
		https://hub.docker.com/v2/logout/; \
	exit $$e

docker-delete-all: docker-delete/example-runtime-bundle docker-delete/activiti-cloud-query \
	docker-delete/example-cloud-connector docker-delete/activiti-cloud-modeling
