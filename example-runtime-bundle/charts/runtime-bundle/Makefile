CHART_REPO := $(or $(CHART_REPO),jenkins-x-chartmuseum:8080)
CURRENT=$(shell pwd)
NAME := runtime-bundle
ORG := $(or $(ORG),activiti)
APP_NAME := $(or $(APP_NAME),$(NAME))
OS := $(shell uname)
RELEASE_VERSION := $(or $(VERSION),$(shell cat ../../VERSION))
RELEASE_ARTIFACT := $(or $(RELEASE_ARTIFACT),$(APP_NAME))

GITHUB_CHARTS_REPO := $(or $(GITHUB_CHARTS_REPO),$(shell git config --get remote.origin.url))
GITHUB_CHARTS_BRANCH := $(or $(GITHUB_CHARTS_BRANCH),gh-pages)

build: clean
	helm init --client-only
	helm repo add charts_activiti https://activiti.github.io/activiti-cloud-charts/
	helm repo add activiti-cloud-helm-charts https://activiti.github.io/activiti-cloud-helm-charts/
	helm dependency build
	helm lint

install:  build
	helm install . --name ${NAME}

upgrade: clean build
	helm upgrade ${NAME} .

dry-run: 
	helm install . --name ${NAME} --debug --dry-run

preview: version package dry-run release

delete:
	helm delete --purge ${NAME}

$(NAME)-$(RELEASE_VERSION).tgz: build
	helm package .

package: $(NAME)-$(RELEASE_VERSION).tgz	

clean:
	rm -rf requirements.lock
	rm -rf charts
	rm -rf ${NAME}*.tgz

release: package
	curl  -u $(CHARTMUSEUM_CREDS_USR):$(CHARTMUSEUM_CREDS_PSW) --data-binary "@$(NAME)-$(RELEASE_VERSION).tgz" $(CHART_REPO)/api/charts
	rm -rf ${NAME}*.tgz%

github: package
	$(eval GITHUB_CHARTS_DIR := $(shell basename $(GITHUB_CHARTS_REPO) .git))
	[[ -d $(GITHUB_CHARTS_DIR) ]] || git clone -b "$(GITHUB_CHARTS_BRANCH)" "$(GITHUB_CHARTS_REPO)" $(GITHUB_CHARTS_DIR)
	cp "$(NAME)-$(RELEASE_VERSION).tgz" $(GITHUB_CHARTS_DIR)
	cd $(GITHUB_CHARTS_DIR) && \
	   helm repo index . && \
	   git add . && \
	   git status && \
	   git commit -m "fix:(version) release $(NAME)-$(RELEASE_VERSION).tgz" && \
	   git pull && \
	   git push origin "$(GITHUB_CHARTS_BRANCH)"
	rm -rf $(GITHUB_CHARTS_DIR)

version: 
	sed -i -e "s/version:.*/version: $(RELEASE_VERSION)/" Chart.yaml
	sed -i -e "s|repository: .*|repository: $(DOCKER_REGISTRY)/$(ORG)/$(APP_NAME)|" values.yaml
	sed -i -e "s/tag: .*/tag: $(RELEASE_VERSION)/" values.yaml

tag: version
	git add --all
	git commit -m "release $(RELEASE_VERSION)" --allow-empty # if first release then no verion update is performed
	git tag -fa v$(RELEASE_VERSION) -m "Release version $(RELEASE_VERSION)"
	git push origin v$(RELEASE_VERSION)

updatebot/push-version: 
	@echo Resolving push versions for artifacts........
	$(eval ACTIVITI_CLOUD_RB_VERSION=$(shell mvn help:evaluate -Dexpression=activiti-cloud-runtime-bundle-service.version -q -DforceStdout -f ../../pom.xml))
	$(eval MAVEN_VERSIONS=org.activiti.cloud.examples:example-runtime-bundle $(RELEASE_VERSION) org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies $(ACTIVITI_CLOUD_RB_VERSION))

	@echo Doing updatebot push-version.....
	for number in 1 2; do \
		updatebot -d ../../ push-version --kind maven $(MAVEN_VERSIONS); \
		rm -rf ../../.updatebot-repos/; \
		sleep $$[ ( $$RANDOM % 10 )  + 1 ]s; \
	done
	