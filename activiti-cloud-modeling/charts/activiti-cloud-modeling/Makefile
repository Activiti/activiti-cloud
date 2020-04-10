#CHART_REPO := http://jenkins-x-chartmuseum:8080
CURRENT=$(shell pwd)
NAME := activiti-cloud-modeling
ORG := $(or $(ORG),activiti)
APP_NAME := $(or $(APP_NAME),$(NAME))
OS := $(shell uname)
RELEASE_VERSION := $(shell cat ../../VERSION)
RELEASE_ARTIFACT := $(or $(RELEASE_ARTIFACT),$(APP_NAME))

GITHUB_CHARTS_REPO := $(or $(GITHUB_CHARTS_REPO),$(shell git config --get remote.origin.url))
GITHUB_CHARTS_BRANCH := $(or $(GITHUB_CHARTS_BRANCH),gh-pages)

build: clean
	helm init --client-only
	helm repo add charts_activiti https://activiti.github.io/activiti-cloud-charts/
	helm repo add activiti-cloud-helm-charts https://activiti.github.io/activiti-cloud-helm-charts/
	helm dependency build
	helm lint
	helm install . --name ${NAME} --debug --dry-run

install:  build
	helm install . --name ${NAME}

upgrade: clean build
	helm upgrade ${NAME} .

delete:
	helm delete --purge ${NAME}

clean:
	rm -rf requirements.lock
	rm -rf charts
	rm -rf ${NAME}*.tgz

release: build
	helm package .
#	curl  -u $(CHARTMUSEUM_CREDS_USR):$(CHARTMUSEUM_CREDS_PSW) --data-binary "@$(NAME)-$(shell sed -n 's/^version: //p' Chart.yaml).tgz" $(CHART_REPO)/api/charts
#	rm -rf ${NAME}*.tgz%

github:
	$(eval GITHUB_CHARTS_DIR := $(shell basename $(GITHUB_CHARTS_REPO) .git))
	git clone -b "$(GITHUB_CHARTS_BRANCH)" "$(GITHUB_CHARTS_REPO)" $(GITHUB_CHARTS_DIR)
	cp "$(NAME)-$(RELEASE_VERSION).tgz" $(GITHUB_CHARTS_DIR)
	cd $(GITHUB_CHARTS_DIR) && \
	   helm repo index . && \
	   git add . && \
	   git status && \
	   git commit -m "fix:(version) release $(NAME)-$(RELEASE_VERSION).tgz" && \
	   git push origin "$(GITHUB_CHARTS_BRANCH)"
	rm -rf $(GITHUB_CHARTS_DIR)

version:
	sed -i -e "s/version:.*/version: $(RELEASE_VERSION)/" Chart.yaml
	export RELEASE_VERSION=$(RELEASE_VERSION) && perl -0777 -pi -e '$$version = $$ENV{'RELEASE_VERSION'};s/repository: activiti\/activiti-cloud-modeling\n\s*tag: ([0-9]|[a-zA-Z]|\.)*/repository: activiti\/activiti-cloud-modeling\n    tag: $$version/' values.yaml

tag: version
	git add --all
	git commit -m "release $(RELEASE_VERSION)" --allow-empty # if first release then no verion update is performed
	git tag -fa v$(RELEASE_VERSION) -m "Release version $(RELEASE_VERSION)"
	git push origin v$(RELEASE_VERSION)

updatebot/push-version:
	$(eval MODELING_DEPENDENCIES_VERSION=$(shell mvn help:evaluate -Dexpression=activiti-cloud-modeling-dependencies.version -q -DforceStdout -f ../../pom.xml))
	$(eval MAVEN_VERSIONS=org.activiti.cloud.examples:activiti-cloud-modeling $(RELEASE_VERSION) org.activiti.cloud.modeling:activiti-cloud-modeling-dependencies $(MODELING_DEPENDENCIES_VERSION))
	
	@echo Doing updatebot push-version.....
	for number in 1 2; do \
		updatebot -d ../../ push-version --kind maven $(MAVEN_VERSIONS); \
		rm -rf ../../.updatebot-repos/; \
		sleep $$[ ( $$RANDOM % 10 )  + 1 ]s; \
	done	
