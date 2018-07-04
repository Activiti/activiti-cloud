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
import org.activiti.cloud.qa.model.modeling.Model;
import org.activiti.cloud.qa.model.modeling.ModelingContext;
import org.activiti.cloud.qa.model.modeling.ModelingIdentifier;
import org.activiti.cloud.qa.rest.DirtyContextHandler;
import org.activiti.cloud.qa.rest.EnableDirtyContext;
import org.activiti.cloud.qa.rest.feign.FeignRestDataClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling context steps
 */
@EnableDirtyContext
public abstract class ModelingContextSteps<M extends ModelingContext> {

    private static final String MODELING_CURRENT_CONTEXT = "modelingCurrentContext";

    @Autowired
    private DirtyContextHandler dirtyContextHandler;

    protected Resource<M> create(M m) {
        service().create(m);
        return dirty(service().findById(m.getId()));
    }

    @Step
    public void addToCurrentContext(Resource<? extends ModelingContext> objectToAdd) {
        getCurrentModelingContext()
                .ifPresent(resource -> {
                    objectToAdd.getContent().getRel()
                            .map(resource::getLink)
                            .map(Link::getHref)
                            .ifPresent(parentUri -> {
                                String childUri = objectToAdd.getLink(REL_SELF).getHref();
                                service().addRelationByUri(parentUri,
                                                           childUri);
                                dirtyRelation(parentUri,
                                              childUri);
                            });
                });
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
        return getCurrentModelingContext()
                .map(this::getRelUri)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .flatMap(resources -> resources
                        .stream()
                        .map(Resource::getContent)
                        .filter(identifier)
                        .findFirst())
                .orElse(null);
    }

    protected Optional<String> getRelUri(Resource<? extends ModelingContext> resource) {
        return getRel()
                .map(resource::getLink)
                .map(Link::getHref);
    }

    protected void updateCurrentModelingObject() {
        getCurrentModelingContext()
                .map(resource -> resource.getLink(REL_SELF))
                .map(Link::getHref)
                .map(this::findByUri)
                .ifPresent(this::setCurrentModelingObject);
    }

    protected Optional<Resource<? extends ModelingContext>> getCurrentModelingContext() {
        return Optional.ofNullable(Serenity.sessionVariableCalled(MODELING_CURRENT_CONTEXT));
    }

    protected Resource<M> checkAndGetCurrentContext(Class<M> expetedCurrentContextClass) {
        Optional<Resource<? extends ModelingContext>> currentModel = getCurrentModelingContext();
        assertThat(currentModel).isNotEmpty();
        assertThat(currentModel.get().getContent()).isInstanceOf(expetedCurrentContextClass);
        return (Resource<M>) currentModel.get();
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
        return getCurrentModelingContext()
                .map(this::getRelUri)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .orElseGet(() -> findAll().getContent());
    }

    protected boolean existsInCurrentContext(ModelingIdentifier identifier) {
        return getCurrentModelingContext()
                .map(this::getRelUri)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .map(resources -> existsInCollection(identifier,
                                                     resources))
                .orElse(false);
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

    protected Resource<M> dirty(Resource<M> resource) {
        return dirtyContextHandler.dirty(resource);
    }

    protected String dirtyRelation(String dirtyRelation,
                                   String childUri) {
        return dirtyContextHandler.dirtyRelation(dirtyRelation,
                                                 childUri);
    }

    protected String dirty(String uri) {
        return dirtyContextHandler.dirty(uri);
    }

    public static void resetCurrentModelingObject() {
        Serenity.setSessionVariable(MODELING_CURRENT_CONTEXT).to(null);
    }

    protected Resource<M> findByUri(String uri) {
        return service().findByUri(uri);
    }

    protected PagedResources<Resource<M>> findAllByUri(String uri) {
        return service().findAllByUri(uri);
    }

    protected PagedResources<Resource<M>> findAll() {
        return service().findAll();
    }

    protected abstract Optional<String> getRel();

    public abstract <S extends FeignRestDataClient<S, M>> S service();
}
