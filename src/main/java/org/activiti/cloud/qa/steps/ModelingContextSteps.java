/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.qa.steps;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.modeling.ModelingContext;
import org.activiti.cloud.qa.model.modeling.ModelingIdentifier;
import org.activiti.cloud.qa.rest.DirtyContext;
import org.activiti.cloud.qa.rest.feign.FeignRestDataClient;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling context steps
 */
public abstract class ModelingContextSteps<M extends ModelingContext> implements DirtyContext {

    private static final String MODELING_CURRENT_CONTEXT = "modelingCurrentContext";

    private static final String DIRTY_CONTEXT = "dirtyContext";

    private static final String DIRTY_CONTEXT_DELIMITER = ";";

    protected Resource<M> create(M m) {
        service().create(m);
        return dirty(service().findById(m.getId()));
    }

    @Step
    public void addToCurrentContext(Resource<? extends ModelingContext> objectToAdd) {
        Resource<? extends ModelingContext> currentContext = getCurrentModelingContext();
        if (currentContext != null) {
            service().addRelationByUri(
                    currentContext.getLink(objectToAdd.getContent().getRel()).getHref(),
                    objectToAdd.getLink(REL_SELF).getHref());
        }
    }

    @Step
    public void checkExists(ModelingIdentifier identifier) {
        assertThat(exists(identifier)).isTrue();
    }

    @Step
    public void checkExistsInCurrentContext(ModelingIdentifier identifier) {
        assertThat(existsInCurrentContext(identifier)).isTrue();
    }

    @Step
    public void deleteAll(ModelingIdentifier identifier) {
        service().findAll()
                .getContent()
                .stream()
                .filter(resourceGroup -> identifier.test(resourceGroup.getContent()))
                .map(resourceGroup -> resourceGroup.getLink(REL_SELF))
                .map(Link::getHref)
                .collect(Collectors.toList())
                .forEach(service()::deleteByUri);
    }

    @Step
    public boolean exists(ModelingIdentifier identifier) {
        return existsInCollection(identifier,
                                  service().findAll().getContent());
    }

    protected M findInCurrentContext(ModelingIdentifier identifier) {
        Resource<? extends ModelingContext> currentObject = getCurrentModelingContext();
        return service().findAllByUri(currentObject.getLink(getRel()).getHref())
                .getContent()
                .stream()
                .map(Resource::getContent)
                .filter(identifier)
                .findFirst()
                .orElse(null);
    }

    protected void updateCurrentModelingObject() {
        Resource<? extends ModelingContext> currentModelingContext = getCurrentModelingContext();
        if (currentModelingContext != null) {
            currentModelingContext = service().findByUri(currentModelingContext.getLink(REL_SELF).getHref());
            setCurrentModelingObject(currentModelingContext);
        }
    }

    protected String getCurrentContextUri() {
        return getCurrentModelingContext().getLink(getRel()).getHref();
    }

    protected Resource<? extends ModelingContext> getCurrentModelingContext() {
        return Serenity.sessionVariableCalled(MODELING_CURRENT_CONTEXT);
    }

    protected void setCurrentModelingObject(Resource<? extends ModelingContext> currentModelingObject) {
        Serenity.setSessionVariable(MODELING_CURRENT_CONTEXT)
                .to(currentModelingObject);
    }

    public void openModelingObject(ModelingIdentifier identifier) {
        Optional<Resource<M>> currentModelingObject = getAvailableModelingObjects()
                .stream()
                .filter(modelingObject -> identifier.test(modelingObject.getContent()))
                .findFirst();
        assertThat(currentModelingObject.isPresent()).isTrue();

        setCurrentModelingObject(currentModelingObject.get());
    }

    protected Collection<Resource<M>> getAvailableModelingObjects() {
        return getCurrentModelingContext() != null ?
                service().findAllByUri(getCurrentContextUri()).getContent() :
                service().findAll().getContent();
    }

    protected boolean existsInCurrentContext(ModelingIdentifier identifier) {
        return existsInCollection(identifier,
                                  service()
                                          .findAllByUri(getCurrentContextUri())
                                          .getContent());
    }

    protected boolean existsInCollection(ModelingIdentifier identifier,
                                         Collection<Resource<M>> modelingObjects) {
        return modelingObjects
                .stream()
                .map(Resource::getContent)
                .filter(identifier)
                .findFirst()
                .isPresent();
    }

    private Resource<M> dirty(Resource<M> resource) {
        String resourceUri = resource.getLink(REL_SELF).getHref();
        Serenity.setSessionVariable(DIRTY_CONTEXT)
                .to(Serenity.hasASessionVariableCalled(DIRTY_CONTEXT) ?
                            String.join(DIRTY_CONTEXT_DELIMITER,
                                        resourceUri,
                                        Serenity.sessionVariableCalled(DIRTY_CONTEXT)) :
                            resourceUri);
        return resource;
    }

    public void clearDirtyContext() {
        if (Serenity.hasASessionVariableCalled(DIRTY_CONTEXT)) {
            String dirtyContext = Serenity.sessionVariableCalled(DIRTY_CONTEXT);
            String[] dirtyUris = dirtyContext.split(DIRTY_CONTEXT_DELIMITER);
            for (String uri : dirtyUris) {
                try {
                    service().deleteByUri(uri);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //ignore any error during cleaning dirty context and go further
                }
            }

            Serenity.setSessionVariable(DIRTY_CONTEXT).to(null);
        }
    }

    public static void resetCurrentModelingObject() {
        Serenity.setSessionVariable(MODELING_CURRENT_CONTEXT).to(null);
    }

    protected abstract String getRel();

    public abstract <S extends FeignRestDataClient<S, M>> S service();
}
