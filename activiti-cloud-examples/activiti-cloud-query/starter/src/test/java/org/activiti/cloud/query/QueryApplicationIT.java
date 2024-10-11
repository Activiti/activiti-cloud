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
package org.activiti.cloud.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = { QueryApplication.class },
    properties = "identity.test.token-interceptor.enabled=false",
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
public class QueryApplicationIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @Test
    public void contextLoads() throws Exception {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    public void defaultSpecificationFileShouldBeAlfrescoFormat() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc
            .perform(MockMvcRequestBuilders.get("/v3/api-docs/Query").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .string(
                        both(notNullValue(String.class))
                            .and(containsString("ListResponseContentCloudProcessDefinition"))
                            .and(containsString("EntriesResponseContentCloudProcessDefinition"))
                            .and(containsString("EntryResponseContentCloudProcessDefinition"))
                            .and(not(containsString("PagedModel")))
                            .and(not(containsString("ResourcesOfResource")))
                            .and(not(containsString("Resource")))
                    )
            );
    }

    @Test
    void shouldUseAlfrescoDbpRestFormat_whenGetProcessInstancesWithAcceptApplicationJson() {
        var responseEntity = testRestTemplate.exchange(
            "/v1/process-instances",
            HttpMethod.GET,
            entityWithAcceptJsonContentHeaders(entityWithAuthorizationHeader("testuser", "password")),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(responseEntity.getBody())
            .isNotEmpty()
            .containsKey("list")
            .extracting("list")
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
            .containsKeys("entries", "pagination");
    }

    private HttpEntity entityWithAuthorizationHeader(String user, String password) {
        HttpEntity authEntity = identityTokenProducer.entityWithAuthorizationHeader(user, password);
        return new HttpEntity(authEntity.getHeaders());
    }

    private HttpEntity entityWithAcceptJsonContentHeaders(HttpEntity authEntity) {
        var headers = new HttpHeaders();
        headers.set("Authorization", authEntity.getHeaders().getFirst("Authorization"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity(headers);
    }
}
