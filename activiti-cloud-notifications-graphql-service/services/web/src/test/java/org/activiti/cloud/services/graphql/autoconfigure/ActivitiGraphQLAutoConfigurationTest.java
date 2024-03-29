/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.graphql.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.introproventures.graphql.jpa.query.schema.GraphQLExecutor;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaExecutor;
import com.introproventures.graphql.jpa.query.web.GraphQLController;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ActivitiGraphQLAutoConfigurationTest {

    @MockBean
    private GraphQLSchema graphQLSchema;

    @Autowired
    private ActivitiGraphQLWebProperties graphQLProperties;

    @Autowired
    private GraphQLExecutor graphQLExecutor;

    @Autowired
    private GraphQLController graphQLController;

    @SpringBootApplication
    static class Application {}

    @Test
    public void contextIsAutoConfigured() {
        assertThat(graphQLExecutor).isInstanceOf(GraphQLJpaExecutor.class);
        assertThat(graphQLController).isNotNull();
        assertThat(graphQLProperties).isNotNull();

        assertThat(graphQLProperties.getPath()).isEqualTo("/graphql");
        assertThat(graphQLProperties.isEnabled()).isEqualTo(true);
    }
}
