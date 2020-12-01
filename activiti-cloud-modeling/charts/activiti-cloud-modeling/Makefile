CURRENT=$(shell pwd)
NAME := activiti-cloud-modeling
ORG := $(or $(ORG),activiti)
APP_NAME := $(or $(APP_NAME),$(NAME))
OS := $(shell uname)
RELEASE_ARTIFACT := $(or $(RELEASE_ARTIFACT),$(APP_NAME))

GITHUB_CHARTS_REPO := $(or $(GITHUB_CHARTS_REPO),$(shell git config --get remote.origin.url))
GITHUB_CHARTS_BRANCH := $(or $(GITHUB_CHARTS_BRANCH),gh-pages)

build: clean
	helm dep up
	helm lint
install:
	helm install . ${NAME}

upgrade: clean build
	helm upgrade ${NAME} .

delete:
	helm delete ${NAME} --namespace ${PREVIEW_NAMESPACE}

clean:
	rm -rf requirements.lock
	rm -rf charts
	rm -rf ${NAME}*.tgz

release:
	helm package .

github:
	$(eval GITHUB_CHARTS_DIR := $(shell basename $(GITHUB_CHARTS_REPO) .git))
	rm -rf $$GITHUB_CHARTS_DIR || echo "try to remove $$GITHUB_CHARTS_DIR"
	git clone -b "$(GITHUB_CHARTS_BRANCH)" https://${GITHUB_TOKEN}@github.com/Activiti/activiti-cloud-helm-charts.git $(GITHUB_CHARTS_DIR) --depth=1
	mkdir $(GITHUB_CHARTS_DIR)/new-charts
	cp "$(NAME)-$(VERSION).tgz" $(GITHUB_CHARTS_DIR)/new-charts
	cd $(GITHUB_CHARTS_DIR) && \
	   helm repo index new-charts --merge index.yaml && \
	   cp new-charts/* . && \
	   rm -fr new-charts && \
	   git add . && \
	   git status && \
	   git commit -m "fix:(version) release $(NAME)-$(VERSION).tgz" && \
	   git pull && \
	   git push origin "$(GITHUB_CHARTS_BRANCH)"
	rm -rf $(GITHUB_CHARTS_DIR)

version:
	sed -i -e "s/version:.*/version: $(VERSION)/" Chart.yaml
	sed -i -e "/frontend:/,/backend:/s/tag: .*/tag: $(VERSION)/" values.yaml
	sed -i -e "/backend:/,/  resources:/s/tag: .*/tag: $(VERSION)/" values.yaml

common-helm-chart-version:
	sed -i -e "s/version:.*/version: $(VERSION)/" requirements.yaml

tag:
	git add --all
	git commit -m "release $(VERSION)" --allow-empty # if first release then no verion update is performed
	git tag -fa v$(VERSION) -m "Release version $(VERSION)"
	git push origin v$(VERSION)

updatebot/push-version:
	$(eval MODELING_DEPENDENCIES_VERSION=$(shell mvn help:evaluate -Dexpression=activiti-cloud-modeling-dependencies.version -q -DforceStdout -f ../../pom.xml))
	$(eval MAVEN_VERSIONS=org.activiti.cloud.examples:activiti-cloud-modeling $(RELEASE_VERSION) org.activiti.cloud.modeling:activiti-cloud-modeling-dependencies $(MODELING_DEPENDENCIES_VERSION))

	updatebot -d ../../ push-version --kind maven $(MAVEN_VERSIONS);

