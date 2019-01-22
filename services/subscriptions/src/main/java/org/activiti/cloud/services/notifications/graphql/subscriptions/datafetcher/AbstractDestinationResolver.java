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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.schema.DataFetchingEnvironment;

public abstract class AbstractDestinationResolver implements DataFetcherDestinationResolver {

    public AbstractDestinationResolver() {
    }
    
    protected abstract String any();
    protected abstract String wildcard();
    protected abstract String path();

	@Override
	public List<String> resolveDestinations(DataFetchingEnvironment environment) {
		String fieldName = resolveFieldName(environment);
		
        String[] argumentNames = resolveArgumentNames(environment);
		
		List<String> destinations = new ArrayList<>();

		String destination = any();

		// Build stomp destination from arguments
		if(environment.getArguments().size() > 0) {

		    destination = Stream.of(argumentNames)
		        .map(name -> resolveArgument(environment, name))
                .map(value -> value.orElse(wildcard()))
                .collect(Collectors.joining(path()));
        }

        destinations.add(fieldName+path()+destination);

		return destinations;
	}
	
	protected String resolveFieldName(DataFetchingEnvironment environment) {
        return environment.getFields().iterator().next().getName();
	    
	}
	

	protected String[] resolveArgumentNames(DataFetchingEnvironment environment) {
        return environment.getFieldDefinition()
                .getArguments()
                .stream()
                .map(arg -> arg.getName())
                .toArray(String[]::new);
	}
	

	private Optional<String> resolveArgument(DataFetchingEnvironment environment, String arumentName) {
	    return Optional.ofNullable(environment.getArgument(arumentName));
	}


}
