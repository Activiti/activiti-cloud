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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import static org.activiti.cloud.organization.core.model.Model.ModelType.FORM;
import static org.activiti.cloud.organization.core.model.Model.ModelType.PROCESS_MODEL;
import static org.activiti.cloud.organization.core.rest.context.RestContext.ACTIVITI;
import static org.activiti.cloud.organization.core.util.Maps.entriesToMap;
import static org.activiti.cloud.organization.core.util.Maps.entry;

/**
 * Provider for rest resource contexts
 */
@Component
public class RestContextProvider {

    //TODO: load the available rest context from property files
    public static final String PROCESS_MODEL_URL = "http://localhost:8088";

    public static final String FORM_MODEL_URL = "http://localhost:8088";

    private static final RestResourceContextItem ACTIVITI_PROCESS_MODELS =
            new RestResourceContextItem("process-models",
                                        PROCESS_MODEL_URL);
    private static final RestResourceContextItem ACTIVITI_FORMS =
            new RestResourceContextItem("forms",
                                        FORM_MODEL_URL);

    private static final RestResourceContext ACTIVITI_CONTEXT =
            new RestResourceContext(ACTIVITI,
                                    Collections.unmodifiableMap(Stream.of(
                                            entry(PROCESS_MODEL,
                                                  ACTIVITI_PROCESS_MODELS),
                                            entry(FORM,
                                                  ACTIVITI_FORMS)).
                                            collect(entriesToMap()))
            );

    private static final Map<RestContext, RestResourceContext> CONTEXT =
            Collections.unmodifiableMap(Stream.of(
                    entry(ACTIVITI,
                          ACTIVITI_CONTEXT)).
                    collect(entriesToMap()));

    /**
     * Get the rest context corresponding to a context key.
     * @param contextKey the context key
     * @return he rest context
     */
    public RestResourceContext getContext(RestContext contextKey) {
        return CONTEXT.get(contextKey);
    }
}
