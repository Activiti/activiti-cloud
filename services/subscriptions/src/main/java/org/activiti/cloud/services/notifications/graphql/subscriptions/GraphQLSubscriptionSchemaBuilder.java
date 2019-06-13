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
package org.activiti.cloud.services.notifications.graphql.subscriptions;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Optional;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.core.io.DefaultResourceLoader;

public class GraphQLSubscriptionSchemaBuilder {

    private GraphQLSchema graphQLSchema = null;

    private final TypeDefinitionRegistry typeRegistry;
    private final  RuntimeWiring.Builder wiring;

    public GraphQLSubscriptionSchemaBuilder(String schemaFileName) {
        //
        // reads a file that provides the schema types
        //
        Reader streamReader;
        try {
            streamReader = loadSchemaFile(schemaFileName);
        } catch (IOException cause) {
            throw new RuntimeException(cause);
        }
        this.typeRegistry = new SchemaParser().parse(streamReader);
        
        this.wiring = RuntimeWiring.newRuntimeWiring()
                                   .scalar(new ObjectScalar());
   }

    private GraphQLSchema buildSchema() {
        return new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring.build());
    }

    public TypeRuntimeWiring.Builder withTypeWiring(String typeName) {
    	TypeRuntimeWiring.Builder builder = newTypeWiring(typeName);

    	wiring.type(builder);

    	return builder;
    }

    public TypeRuntimeWiring.Builder withSubscription(String fieldName, DataFetcher<?> dataFetcher) {
    	TypeRuntimeWiring.Builder builder = newTypeWiring("Subscription");

    	wiring.type(builder.dataFetcher(fieldName, dataFetcher));

    	return builder;
    }


    public GraphQLSchema getGraphQLSchema() {
        return Optional.ofNullable(graphQLSchema)
        		.orElseGet(this::buildSchema);
    }

    protected Reader loadSchemaFile(String name) throws IOException {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        InputStream stream = resourceLoader.getResource(name).getInputStream();
        return new InputStreamReader(stream);
    }

}
