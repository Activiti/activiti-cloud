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
package org.activiti.cloud.services.query.qraphql.ws.datafetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.schema.DataFetchingEnvironment;

public class GraphQLStompRelayDataFetcherDestinationResolver implements StompRelayDestinationResolver {

    private static final String HASH = "#";
    private static final String WILDCARD = "*";
    private static final String DOT = ".";

    private final String[] argumentNames;

    public GraphQLStompRelayDataFetcherDestinationResolver(String[] argumentNames) {
        this.argumentNames = argumentNames;
    }

	@Override
	public List<String> resolveDestinations(DataFetchingEnvironment environment) {
		String fieldName = environment.getFields().iterator().next().getName();
		List<String> destinations = new ArrayList<>();

		String destination = HASH;

		// Build stomp destination from arguments
		if(environment.getArguments().size() > 0) {

		    destination = Stream.of(argumentNames)
		        .map(name -> resolveArgument(environment, name))
                .map(value -> value.orElse(WILDCARD))
                .collect(Collectors.joining(DOT));
        }

        destinations.add(fieldName+DOT+destination);

		return destinations;
	}

	private Optional<String> resolveArgument(DataFetchingEnvironment environment, String arumentName) {
	    return Optional.ofNullable(environment.getArgument(arumentName));
	}


}
