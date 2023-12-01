/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.acc.modeling.steps;

import static org.activiti.cloud.modeling.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.common.util.HttpUtils.HEADER_ATTACHEMNT_FILENAME;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

import feign.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.modeling.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.acc.modeling.modeling.ModelingContextHandler;
import org.activiti.cloud.acc.modeling.modeling.ModelingIdentifier;
import org.activiti.cloud.acc.shared.rest.DirtyContextHandler;
import org.activiti.cloud.acc.shared.rest.EnableDirtyContext;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.services.common.file.FileContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.StreamUtils;

/**
 * Modeling context steps
 */
@EnableDirtyContext
public abstract class ModelingContextSteps<M> {

    @Autowired
    protected ModelingContextHandler modelingContextHandler;

    @Autowired
    private DirtyContextHandler dirtyContextHandler;

    @Autowired
    private ModelingTestsConfigurationProperties config;

    protected EntityModel<M> create(M m) {
        EntityModel<M> model = modelingContextHandler
            .getCurrentModelingContext()
            .flatMap(this::getRelUri)
            .map(this::modelingUri)
            .map(uri -> service().createByUri(uri, m))
            .orElseGet(() -> service().create(m));
        return dirty(model);
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
        service()
            .findAll()
            .getContent()
            .stream()
            .filter(resource -> identifier.test(resource.getContent()))
            .map(resource -> resource.getLink(SELF).get())
            .map(Link::getHref)
            .map(this::modelingUri)
            .collect(Collectors.toList())
            .forEach(service()::deleteByUri);
    }

    @Step
    public boolean exists(ModelingIdentifier<M> identifier) {
        return existsInCollection(identifier, service().findAll().getContent());
    }

    protected Optional<String> getRelUri(EntityModel<?> entityModel) {
        return getRel().map(entityModel::getLink).map(Optional::get).map(Link::getHref).map(this::cutQueryParams);
    }

    protected String cutQueryParams(String uri) {
        int index = uri.indexOf('{');
        if (index <= 0) {
            return uri;
        }
        return uri.substring(0, index);
    }

    protected void updateCurrentModelingObject() {
        modelingContextHandler
            .getCurrentModelingContext()
            .map(resource -> resource.getLink(SELF).get())
            .map(Link::getHref)
            .map(this::modelingUri)
            .map(this::findByUri)
            .ifPresent(modelingContextHandler::setCurrentModelingObject);
    }

    protected EntityModel<M> checkAndGetCurrentContext(Class<M> expectedCurrentContextClass) {
        Optional<EntityModel<?>> optionalModelingContext = modelingContextHandler.getCurrentModelingContext();
        assertThat(
            optionalModelingContext
                .map(EntityModel::getContent)
                .filter(expectedCurrentContextClass::isInstance)
                .map(expectedCurrentContextClass::cast)
        )
            .isNotEmpty();
        return (EntityModel<M>) optionalModelingContext.get();
    }

    @Step
    public void openModelingObject(ModelingIdentifier<M> identifier) {
        Optional<EntityModel<M>> currentModelingObject = getAvailableModelingObjects()
            .stream()
            .filter(modelingObject -> identifier.test(modelingObject.getContent()))
            .findFirst()
            .map(resource -> resource.getLink(SELF).get())
            .map(Link::getHref)
            .map(this::modelingUri)
            .map(this::findByUri);
        assertThat(currentModelingObject.isPresent()).isTrue();

        modelingContextHandler.setCurrentModelingObject(currentModelingObject.get());
    }

    protected Collection<EntityModel<M>> getAvailableModelingObjects() {
        return modelingContextHandler
            .getCurrentModelingContext()
            .flatMap(this::getRelUri)
            .map(this::findAllByUri)
            .map(PagedModel::getContent)
            .orElseGet(() -> findAll().getContent());
    }

    @Step
    public boolean existsInCurrentContext(ModelingIdentifier<M> identifier) {
        return modelingContextHandler
            .getCurrentModelingContext()
            .flatMap(this::getRelUri)
            .map(this::findAllByUri)
            .map(PagedModel::getContent)
            .map(resources -> existsInCollection(identifier, resources))
            .orElse(false);
    }

    protected boolean existsInCollection(ModelingIdentifier<M> identifier, Collection<EntityModel<M>> modelingObjects) {
        return modelingObjects
            .stream()
            .map(EntityModel::getContent)
            .filter(Objects::nonNull)
            .filter(identifier)
            .findFirst()
            .isPresent();
    }

    protected EntityModel<M> dirty(EntityModel<M> resource) {
        dirtyContextHandler.dirty(modelingUri(resource.getLink(SELF).get().getHref()));
        return resource;
    }

    protected EntityModel<M> findByUri(String uri) {
        return service().findByUri(uri);
    }

    protected PagedModel<EntityModel<M>> findAllByUri(String uri) {
        return service().findAllByUri(modelingUri(uri));
    }

    protected PagedModel<EntityModel<M>> findAll() {
        return service().findAll();
    }

    protected FileContent toFileContent(Response response) throws IOException {
        String contentType = Optional
            .ofNullable(response.headers().get("Content-Type"))
            .map(contentTypes -> contentTypes.stream().map(Object::toString).findFirst().orElse(null))
            .orElseThrow(() -> new RuntimeException("No Content-Type header in feign response"));

        String filename = Optional
            .ofNullable(response.headers().get("Content-Disposition"))
            .map(contentTypes ->
                contentTypes
                    .stream()
                    .map(Object::toString)
                    .findFirst()
                    .filter(contentDisposition -> contentDisposition.startsWith(HEADER_ATTACHEMNT_FILENAME))
                    .map(contentDisposition -> contentDisposition.substring(HEADER_ATTACHEMNT_FILENAME.length()))
                    .orElse(null)
            )
            .orElseThrow(() -> new RuntimeException("No Content-Disposition header in feign response"));
        return new FileContent(filename, contentType, StreamUtils.copyToByteArray(response.body().asInputStream()));
    }

    protected String modelingUri(String uri) {
        return uri.replace(
            String.format("%s://%s", config.getGatewayProtocol(), config.getModelingPath()),
            config.getModelingUrl()
        );
    }

    public ModelType getModelType(String modelType) {
        if (modelType.equalsIgnoreCase(PROCESS)) {
            return new ProcessModelType();
        }
        throw new RuntimeException("Unknown model type: " + modelType);
    }

    protected abstract Optional<String> getRel();

    public abstract <S extends FeignRestDataClient<S, M>> S service();
}
