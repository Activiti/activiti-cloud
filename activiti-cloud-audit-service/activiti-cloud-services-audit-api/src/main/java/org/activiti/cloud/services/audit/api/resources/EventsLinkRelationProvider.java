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

package org.activiti.cloud.services.audit.api.resources;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.LinkRelationProvider;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class EventsLinkRelationProvider implements LinkRelationProvider {

    public static final String COLLECTION_RESOURCE_REL = "events";

    public static final String ITEM_RESOURCE_REL = "event";

    private static final LinkRelation collectionResourceRel = LinkRelation.of(COLLECTION_RESOURCE_REL);

    private static final LinkRelation itemResourceRel = LinkRelation.of(ITEM_RESOURCE_REL);

    @Override
    public LinkRelation getItemResourceRelFor(Class<?> aClass) {
        return itemResourceRel;
    }

    @Override
    public LinkRelation getCollectionResourceRelFor(Class<?> aClass) {
        return collectionResourceRel;
    }

    @Override
    public boolean supports(LookupContext delimiter) {
        return CloudRuntimeEvent.class.isAssignableFrom(delimiter.getType());
    }
}
