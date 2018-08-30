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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.qa.model.modeling.ModelingContextHandler;
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
public abstract class ModelingContextSteps<M> {

    @Autowired
    private DirtyContextHandler dirtyContextHandler;

    @Autowired
    private ModelingContextHandler modelingContextHandler;

    protected Resource<M> create(String id,
                                 M m) {
        service().create(m);
        return dirty(service().findById(id));
    }

    protected void addToCurrentContext(Resource<?> objectToAdd,
                                       Optional<String> objectToAddRel) {
        modelingContextHandler
                .getCurrentModelingContext()
                .ifPresent(resource -> {
                    objectToAddRel
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
    public void checkExists(ModelingIdentifier<M> identifier) {
        assertThat(exists(identifier)).isTrue();
    }

    @Step
    public void checkExistsInCurrentContext(ModelingIdentifier<M> identifier) {
        assertThat(existsInCurrentContext(identifier)).isTrue();
    }

    @Step
    public void deleteAll(ModelingIdentifier<M> identifier) {
        service().findAll()
                .getContent()
                .stream()
                .filter(resource -> identifier.test(resource.getContent()))
                .map(resource -> resource.getLink(REL_SELF))
                .map(Link::getHref)
                .collect(Collectors.toList())
                .forEach(service()::deleteByUri);
    }

    @Step
    public boolean exists(ModelingIdentifier<M> identifier) {
        return existsInCollection(identifier,
                                  service().findAll().getContent());
    }

    protected M findInCurrentContext(ModelingIdentifier<M> identifier) {
        return modelingContextHandler
                .getCurrentModelingContext()
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

    protected Optional<String> getRelUri(Resource<?> resource) {
        return getRel()
                .map(resource::getLink)
                .map(Link::getHref);
    }

    protected void updateCurrentModelingObject() {
        modelingContextHandler
                .getCurrentModelingContext()
                .map(resource -> resource.getLink(REL_SELF))
                .map(Link::getHref)
                .map(this::findByUri)
                .ifPresent(modelingContextHandler::setCurrentModelingObject);
    }

    protected Resource<M> checkAndGetCurrentContext(Class<M> expetedCurrentContextClass) {
        Optional<Resource<?>> currentModelingContext =
                modelingContextHandler.getCurrentModelingContext();
        assertThat(currentModelingContext).isNotEmpty();
        assertThat(currentModelingContext.get().getContent()).isInstanceOf(expetedCurrentContextClass);
        return (Resource<M>) currentModelingContext.get();
    }

    public void openModelingObject(ModelingIdentifier<M> identifier) {
        Optional<Resource<M>> currentModelingObject = getAvailableModelingObjects()
                .stream()
                .filter(modelingObject -> identifier.test(modelingObject.getContent()))
                .findFirst();
        assertThat(currentModelingObject.isPresent()).isTrue();

        modelingContextHandler.setCurrentModelingObject(currentModelingObject.get());
    }

    protected Collection<Resource<M>> getAvailableModelingObjects() {
        return modelingContextHandler
                .getCurrentModelingContext()
                .map(this::getRelUri)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .orElseGet(() -> findAll().getContent());
    }

    protected boolean existsInCurrentContext(ModelingIdentifier<M> identifier) {
        return modelingContextHandler
                .getCurrentModelingContext()
                .map(this::getRelUri)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::findAllByUri)
                .map(PagedResources::getContent)
                .map(resources -> existsInCollection(identifier,
                                                     resources))
                .orElse(false);
    }

    protected boolean existsInCollection(ModelingIdentifier<M> identifier,
                                         Collection<Resource<M>> modelingObjects) {
        return modelingObjects
                .stream()
                .map(Resource::getContent)
                .filter(Objects::nonNull)
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
