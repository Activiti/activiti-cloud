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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.reactivestreams.Publisher;
import reactor.util.context.Context;

public class EngineEventsPublisherDataFetcher implements DataFetcher<Publisher<List<EngineEvent>>>{

	private final EngineEventsPublisherFactory publisherFactory;

	public EngineEventsPublisherDataFetcher(EngineEventsPublisherFactory publisherFactory) {
		 this.publisherFactory = publisherFactory;
	}

	@Override
	public Publisher<List<EngineEvent>> get(DataFetchingEnvironment environment) {

        return publisherFactory.getPublisher(environment)
                               .subscriberContext(Context.of(DataFetchingEnvironment.class, environment));
    }

}
