/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.organization.core.rest.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Rest resources context.
 */
public class RestResourceContext extends HashMap<Object, RestResourceContextItem> {

    private RestContext restContext;

    public RestResourceContext(RestContext restContext,
                               RestResourceContextItem restResourceContextItem) {
        put(restContext,
            restResourceContextItem);
        this.restContext = restContext;
    }

    public RestResourceContext(RestContext restContext,
                               Map<Object, RestResourceContextItem> restResourcesMap) {
        super(restResourcesMap);
        this.restContext = restContext;
    }

    public RestResourceContextItem getResource(Object resourceKey) {
        return resourceKey != null ? get(resourceKey) : get(restContext);
    }

    public boolean isExternal() {
        return restContext.isExternal();
    }
}
