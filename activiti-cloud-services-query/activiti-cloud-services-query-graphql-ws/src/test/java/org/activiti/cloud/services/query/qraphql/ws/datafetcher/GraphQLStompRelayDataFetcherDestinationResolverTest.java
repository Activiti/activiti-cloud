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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import org.junit.Before;
import org.junit.Test;


public class GraphQLStompRelayDataFetcherDestinationResolverTest {

    private GraphQLStompRelayDataFetcherDestinationResolver testSubject;

    private GraphQLFieldDefinition fieldDefinition;

    @Before
    public void setUp() throws Exception {
        this.testSubject = new GraphQLStompRelayDataFetcherDestinationResolver(new String[] {"arg1", "arg2"});

        this.fieldDefinition = GraphQLFieldDefinition.newFieldDefinition()
                .name("Field")
                .type(mock(GraphQLOutputType.class))
                .argument(GraphQLArgument.newArgument()
                          .name("arg1")
                          .type(mock(GraphQLInputType.class))
                          .build())
                .argument(GraphQLArgument.newArgument()
                          .name("arg2")
                          .type(mock(GraphQLInputType.class))
                          .build())
                .build();
    }

    @Test
    public void testResolveDestinations() {
        // given
        DataFetchingEnvironment environment = new DataFetchingEnvironmentBuilder()
                .fieldDefinition(fieldDefinition)
                .fields(Arrays.asList(new Field("Field")))
                .arguments(new HashMap<String, Object>() {
                    {
                        put("arg1","value1");
                        put("arg2","value2");
                    }
                })
                .build();

        // when
        List<String> destinations = testSubject.resolveDestinations(environment);

        // then
        assertThat(destinations).containsExactly("Field.value1.value2");
    }

    @Test
    public void testResolveDestinationsNoArguments() {
        // given
        DataFetchingEnvironment environment = new DataFetchingEnvironmentBuilder()
                .fieldDefinition(fieldDefinition)
                .fields(Arrays.asList(new Field("Field")))
                .build();

        // when
        List<String> destinations = testSubject.resolveDestinations(environment);

        // then
        assertThat(destinations).containsExactly("Field.#");
    }

    @Test
    public void testResolveDestinationsSkipArgumentsCorrectOrder() {
        // given
        DataFetchingEnvironment environment = new DataFetchingEnvironmentBuilder()
                .fieldDefinition(fieldDefinition)
                .fields(Arrays.asList(new Field("Field")))
                .arguments(new HashMap<String, Object>() {
                    {
                        put("arg2","value2");
                    }
                })
                .build();

        // when
        List<String> destinations = testSubject.resolveDestinations(environment);

        // then
        assertThat(destinations).containsExactly("Field.*.value2");
    }

    @Test
    public void testResolveDestinationsIngoresWrongArguments() {
        // given
        DataFetchingEnvironment environment = new DataFetchingEnvironmentBuilder()
                .fieldDefinition(fieldDefinition)
                .fields(Arrays.asList(new Field("Field")))
                .arguments(new HashMap<String, Object>() {
                    {
                        put("wrong","value2");
                    }
                })
                .build();

        // when
        List<String> destinations = testSubject.resolveDestinations(environment);

        // then
        assertThat(destinations).containsExactly("Field.*.*");
    }

}
