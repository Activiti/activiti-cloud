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

package org.activiti.cloud.services.modeling.rest.assembler;

import org.activiti.cloud.modeling.api.ModelType;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;

/**
 * Rel provider for {@link ModelType}
 */
public class ModelTypeLinkRelationProvider implements LinkRelationProvider {

    public static final String COLLECTION_RESOURCE_REL = "model-types";

    public static final String ITEM_RESOURCE_REL = "model-type";

    public static final LinkRelation collectionResourceRel = LinkRelation.of("model-types");

    public static final LinkRelation itemResourceRel = LinkRelation.of("model-type");

    @Override
    public LinkRelation getItemResourceRelFor(Class<?> type) {
        return itemResourceRel;
    }

    @Override
    public LinkRelation getCollectionResourceRelFor(Class<?> type) {
        return collectionResourceRel;
    }

    @Override
    public boolean supports(LookupContext delimiter) {
        return ModelType.class.isAssignableFrom(delimiter.getType());
    }

}
