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

package org.activiti.cloud.acc.shared.rest;

import static org.springframework.hateoas.IanaLinkRelations.SELF;

import feign.Headers;
import feign.RequestLine;
import feign.gson.GsonEncoder;
import net.serenitybdd.core.Serenity;
import org.activiti.cloud.acc.shared.rest.feign.FeignRestDataClient;
import org.springframework.hateoas.EntityModel;

/**
 * Dirty context handler
 */
public class DirtyContextHandler {

    private static final String DIRTY_CONTEXT = "dirtyContext";

    private static final String DIRTY_CONTEXT_DELIMITER = ";";

    public <M> EntityModel<M> dirty(EntityModel<M> resource) {
        dirty(resource.getLink(SELF).get().getHref());
        return resource;
    }

    public String dirtyRelation(String parentUri, String childUri) {
        return dirty(parentUri + childUri.substring(childUri.lastIndexOf('/')));
    }

    public String dirty(String uri) {
        Serenity.setSessionVariable(DIRTY_CONTEXT)
                .to(Serenity.hasASessionVariableCalled(DIRTY_CONTEXT) ?
                            String.join(DIRTY_CONTEXT_DELIMITER,
                                        uri,
                                        Serenity.sessionVariableCalled(DIRTY_CONTEXT)) :
                            uri);
        return uri;
    }

    public void cleanup() {
        if (Serenity.hasASessionVariableCalled(DIRTY_CONTEXT)) {
            String dirtyContext = Serenity.sessionVariableCalled(DIRTY_CONTEXT);
            String[] dirtyUris = dirtyContext.split(DIRTY_CONTEXT_DELIMITER);

            Serenity.setSessionVariable(DIRTY_CONTEXT).to(null);
            for (String uri : dirtyUris) {
                try {
                    deleteByUri(uri);
                } catch (Exception ex) {
                    //this uri is still dirty
                    dirty(uri);
                }
            }
        }
    }

    public void deleteByUri(String uri) {
        FeignRestDataClient.builder(new GsonEncoder(), new feign.codec.Decoder.Default())
                .target(DeleteByUriClient.class,
                        uri)
                .delete();
    }

    private interface DeleteByUriClient {
        @RequestLine("DELETE")
        @Headers("Content-Type: application/json")
        void delete();
    }
}
