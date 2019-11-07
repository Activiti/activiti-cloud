ACTIVITI_CLOUD_CONNECTORS_VERSION := 7.1.230
ACTIVITI_CLOUD_AUDIT_VERSION := 7.1.223
ACTIVITI_CLOUD_QUERY_VERSION := 7.1.217
ACTIVITI_CLOUD_RB_VERSION := 7.1.261
ACTIVITI_CLOUD_NOTIFICATIONS_VERSION := 7.1.234
ACTIVITI_CLOUD_MODELING :=7.1.442

MODELING_DEPENDENCIES_VERSION := 7.1.234
ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION := 7.1.0


ACTIVITI_CLOUD_VERSION := $(shell cat VERSION)
get-modeling-dependencies-version:
	@echo $(MODELING_DEPENDENCIES_VERSION)
get-acc-scenarious-version:
	@echo $(ACTIVITI_CLOUD_ACCEPTANCE_SCENARIOUS_VERSION)	

acc-tests:	
	git clone 'https://github.com/Activiti/activiti-cloud-acceptance-scenarios.git'               
        git fetch --all --tags --prune
	git checkout tags/v$(ACCEPTANCE_VERSION) -b $(ACCEPTANCE_VERSION)'
	sleep 90
	mvn clean install -DskipTests && mvn -pl 'runtime-acceptance-tests,modeling-acceptance-tests' clean verify"

update-ea:
	#$(eval ACTIVITI_CLOUD_VERSION = $(shell cat VERSION)) 
	@echo "ACTIVITI_CLOUD_VERSION =<$(ACTIVITI_CLOUD_VERSION)>" 

	$(eval ID = $(shell echo ${ACTIVITI_CLOUD_VERSION}${MODELING_DEPENDENCIES_VERSION}|tr -dc '[:alnum:]\n\r'))
	@echo ID=${ID}	
	
	rm -rf alfresco-process-parent||echo removing alfresco-process-parent
	git clone https://oauth2:${GITLAB_TOKEN}@git.alfresco.com/process-services/alfresco-process-parent.git
	@echo "Clone for alfresco-process-parent done"

	cd alfresco-process-parent && \
	  git checkout develop && \
	  git checkout -b update-cloud-to-$(ACTIVITI_CLOUD_VERSION)-$(MODELING_DEPENDENCIES_VERSION) && \
	  mvn versions:set-property -Dproperty=activiti-cloud.version -DnewVersion=$(ACTIVITI_CLOUD_VERSION) && \
	  mvn versions:set-property -Dproperty=activiti-cloud-modeling.version -DnewVersion=$(MODELING_DEPENDENCIES_VERSION) && \
	  git diff --word-diff && \
	  git commit -a -m "AAE-0 update ACTIVITI_CLOUD_VERSION to ${ACTIVITI_CLOUD_VERSION} MODELING_DEPENDENCIES_VERSION to ${MODELING_DEPENDENCIES_VERSION} ACTIVITI-0000" && \
	  git push  --set-upstream origin update-cloud-to-${ACTIVITI_CLOUD_VERSION}-${MODELING_DEPENDENCIES_VERSION} 
	@cd alfresco-process-parent && curl --request POST --header "PRIVATE-TOKEN: $(GITLAB_TOKEN)" --header "Content-Type: application/json" -d '{"id": ${ID} ,"source_branch": "update-cloud-to-${ACTIVITI_CLOUD_VERSION}-${MODELING_DEPENDENCIES_VERSION}" ,"target_branch":"develop","title":"community propagation ACTIVITI_CLOUD_VERSION to ${ACTIVITI_CLOUD_VERSION} MODELING_DEPENDENCIES_VERSION to ${MODELING_DEPENDENCIES_VERSION}"}' https://git.alfresco.com/api/v4/projects/1031/merge_requests	

updatebot/push-version:
	updatebot push-version --kind maven org.activiti.cloud.dependencies:activiti-cloud-dependencies $(VERSION) --merge false
	updatebot push-version --kind helm runtime-bundle $(ACTIVITI_CLOUD_RB_VERSION) activiti-cloud-connector $(ACTIVITI_CLOUD_CONNECTORS_VERSION) \
	 activiti-cloud-query $(ACTIVITI_CLOUD_QUERY_VERSION) activiti-cloud-notifications-graphql $(ACTIVITI_CLOUD_NOTIFICATIONS_VERSION)  \
	 activiti-cloud-audit $(ACTIVITI_CLOUD_AUDIT_VERSION) activiti-cloud-modeling $(ACTIVITI_CLOUD_MODELING)


updatebot/push-version-dry:
	updatebot push-version --kind helm runtime-bundle $(ACTIVITI_CLOUD_RB_VERSION) activiti-cloud-connector $(ACTIVITI_CLOUD_CONNECTORS_VERSION) \
	 activiti-cloud-query $(ACTIVITI_CLOUD_QUERY_VERSION) activiti-cloud-notifications-graphql $(ACTIVITI_CLOUD_NOTIFICATIONS_VERSION)  \
	 activiti-cloud-audit $(ACTIVITI_CLOUD_AUDIT_VERSION) activiti-cloud-modeling $(ACTIVITI_CLOUD_MODELING) --dry


prepare-helm-chart:
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
        	helm init --client-only && \
         	helm repo add activiti-cloud-helm-charts https://activiti.github.io/activiti-cloud-helm-charts/ && \
        	helm repo add alfresco https://kubernetes-charts.alfresco.com/stable	&& \
        	helm repo add alfresco-incubator https://kubernetes-charts.alfresco.com/incubator && \
        	helm dependency build && \
        	helm lint 
run-helm-chart:		
	cd  .updatebot-repos/github/activiti/activiti-cloud-full-chart/charts/activiti-cloud-full-example/ && \
            	helm upgrade ${PREVIEW_NAMESPACE} . \
            		--install \
            		--set global.gateway.domain=${GLOBAL_GATEWAY_DOMAIN} \
            		--namespace ${PREVIEW_NAMESPACE} \
            		--debug \
            		--wait
delete:
	helm delete --purge ${PREVIEW_NAMESPACE} || echo "try to remove helm chart"				
