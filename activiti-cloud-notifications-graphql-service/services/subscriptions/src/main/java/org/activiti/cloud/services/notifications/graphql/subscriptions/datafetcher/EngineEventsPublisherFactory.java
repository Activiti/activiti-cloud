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
package org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import reactor.core.publisher.Flux;

public interface EngineEventsPublisherFactory {

    public Flux<List<EngineEvent>> getPublisher(DataFetchingEnvironment environment);

}
