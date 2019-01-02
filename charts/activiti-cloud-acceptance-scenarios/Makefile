CHART_REPO := http://jenkins-x-chartmuseum:8080
CURRENT=$(pwd)
NAME := activiti-cloud-acceptance-scenarios
HELM_RELEASE_NAME := ${HELM_RELEASE}
OS := $(shell uname)
RELEASE_VERSION := $(shell cat ../../VERSION)

build: clean
	helm init --client-only
	helm repo add charts_activiti https://activiti.github.io/activiti-cloud-charts/
	helm repo add activiti-cloud-helm-charts https://activiti.github.io/activiti-cloud-helm-charts/
	rm -rf requirements.lock
	helm dependency build
	helm lint

install: clean build
	sed -i -e "s/jx-staging/${PREVIEW_NAMESPACE}/g" values.yaml
	helm install . --name ${HELM_RELEASE_NAME} --namespace ${PREVIEW_NAMESPACE} --wait

upgrade: clean build
	helm upgrade ${HELM_RELEASE_NAME} .

delete:
	helm delete --purge ${HELM_RELEASE_NAME}

clean:
	rm -rf charts
	rm -rf ${NAME}*.tgz

release: clean
	helm dependency build
	helm lint
	helm package .
#	curl --fail -u $(CHARTMUSEUM_CREDS_USR):$(CHARTMUSEUM_CREDS_PSW) --data-binary "@$(NAME)-$(shell sed -n 's/^version: //p' Chart.yaml).tgz" $(CHART_REPO)/api/charts
	rm -rf ${NAME}*.tgz%

tag:
ifeq ($(OS),Darwin)
	sed -i "" -e "s/version:.*/version: $(RELEASE_VERSION)/" Chart.yaml
	sed -i "" -e "s/tag: .*/tag: $(RELEASE_VERSION)/" values.yaml
else ifeq ($(OS),Linux)
	sed -i -e "s/version:.*/version: $(RELEASE_VERSION)/" Chart.yaml
	sed -i -e "s|repository: .*|repository: $(DOCKER_REGISTRY)\/activiti\/activiti-cloud-acceptance-scenarios|" values.yaml
	sed -i -e "s/tag: .*/tag: $(RELEASE_VERSION)/" values.yaml
else
	echo "platfrom $(OS) not supported to release from"
	exit -1
endif
	git add --all
	git commit -m "release $(RELEASE_VERSION)" --allow-empty # if first release then no verion update is performed
	git tag -fa v$(RELEASE_VERSION) -m "Release version $(RELEASE_VERSION)"
	git push origin v$(RELEASE_VERSION)
