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
package org.activiti.cloud.services.query.graphql.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.introproventures.graphql.jpa.query.schema.GraphQLExecutor;
import com.introproventures.graphql.jpa.query.schema.GraphQLSchemaBuilder;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaExecutor;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaSchemaBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    properties={"spring.activiti.cloud.services.query.graphql.enabled=false"},
    webEnvironment = WebEnvironment.NONE
)
public class EnableActivitiGraphQLQueryServiceTest {

    @Autowired
    private ActivitiGraphQLSchemaProperties graphQLProperties;

    @Autowired
    private GraphQLExecutor graphQLExecutor;

    @Autowired
    private GraphQLSchemaBuilder graphQLSchemaBuilder;

    @SpringBootApplication
    @EnableActivitiGraphQLQueryService
    static class Application {
    }

    @Test
    public void contextIsConfigured() {
        assertThat(graphQLExecutor).isInstanceOf(GraphQLJpaExecutor.class);
        assertThat(graphQLSchemaBuilder).isInstanceOf(GraphQLJpaSchemaBuilder.class);
        assertThat(graphQLProperties).isNotNull();

        assertThat(graphQLProperties.getName()).isEqualTo("Query");
        assertThat(graphQLProperties.getPath()).isEqualTo("/graphql");
        assertThat(graphQLProperties.isEnabled()).isEqualTo(true);
    }

}
