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
package org.activiti.cloud.services.query.qraphql.ws.datafetcher;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import graphql.schema.DataFetchingEnvironment;

public class SimpleStompRelayDataFetcherDestinationResolver implements StompRelayDestinationResolver {

	@SuppressWarnings("unchecked")
	@Override
	public List<String> resolveDestinations(DataFetchingEnvironment environment) {
		String fieldName = environment.getFields().iterator().next().getName();

		// fieldName.argumentName.[argumentValue...]
		List<String> destinations = environment.getArguments().entrySet().stream()
			.filter(arg -> arg.getValue() instanceof Collection)
			.map(arg -> new SimpleEntry<String, Collection<Object>>(fieldName+"."+arg.getKey(),
								(Collection<Object>) arg.getValue()))
			.map(entry -> entry.getValue()
					.stream()
					.map(v -> entry.getKey()+"."+v.toString())
					.collect(Collectors.toList())
			)
			.flatMap(list -> list.stream())
			.collect(Collectors.toList());

		// catch them all
		if(destinations.isEmpty())
			destinations.add(fieldName+".#");

		return destinations;
	}

}
