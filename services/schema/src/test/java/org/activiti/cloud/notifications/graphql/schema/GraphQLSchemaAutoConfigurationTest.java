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
package org.activiti.cloud.notifications.graphql.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLSchemaConfigurer;
import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLShemaRegistration;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.NONE)
public class GraphQLSchemaAutoConfigurationTest {
    
    @Autowired
    private GraphQLSchema graphQLSchema;
    
    @SpringBootApplication
    static class Application {

        @Component
        static class TestGraphQLSchemaConfigurer implements GraphQLSchemaConfigurer {

            @Override
            public void configure(GraphQLShemaRegistration registry) {
                GraphQLObjectType query = GraphQLObjectType.newObject()
                        .name("query")
                        .field(GraphQLFieldDefinition.newFieldDefinition()
                                .name("hello")
                                .type(Scalars.GraphQLString)
                                .dataFetcher(environment -> {
                                    return "world";
                                }))
                        .build(); 
                
                GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
                        .query(query)
                        .build();                
                
                registry.register(graphQLSchema);
            }
        }
    }

    @Test
    public void contextLoads() {
        // given
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

        // when
        Map<String, Object> result = graphQL.execute("{hello}").getData();
        
        // then
        assertThat(result.toString()).isEqualTo("{hello=world}");
    }
    


}
