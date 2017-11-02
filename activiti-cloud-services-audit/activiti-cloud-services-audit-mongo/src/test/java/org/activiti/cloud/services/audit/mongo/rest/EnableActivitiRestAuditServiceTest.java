package org.activiti.cloud.services.audit.mongo.rest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.activiti.cloud.services.test.rest.support.TestMvcClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.core.JsonPathLinkDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class EnableActivitiRestAuditServiceTest {

    private static String basePath = "/v1";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private LinkDiscoverers discoverers;

    private MockMvc mvc;

    private TestMvcClient client;

    @SpringBootApplication
    @ComponentScan("org.activiti.cloud.services.audit.mongo")
    static class Configuration {

        @Bean
        public LinkDiscoverer alpsLinkDiscoverer() {
            return new JsonPathLinkDiscoverer("$.descriptors[?(@.name == '%s')].href",
                                              MediaType.valueOf("application/alps+json"));
        }
    }

    @Before
    public void setUp() {
        this.mvc = MockMvcBuilders.webAppContextSetup(context).build();
        this.client = new TestMvcClient(mvc, discoverers).setBasePath(basePath);
    }

    @Test
    public void contextLoads() {
        // should pass
    }

    @Test
    public void exposeReadOnlyLinks() throws Exception {

        // Given 
        Link profileLink = client.discoverUnique("profile");
        String expectedAlpsVersion = "1.0";

        // When
        Link resourceLink = client.discoverUnique(profileLink, "events", MediaType.ALL);

        // Then
        client.follow(resourceLink, RestMediaTypes.ALPS_JSON)//
              .andExpect(jsonPath("$.alps.version")
                         .value(expectedAlpsVersion))
              .andExpect(jsonPath("$.alps.descriptors[*].id")
                         .value(hasItems("get-events", "get-processEngineEventDocument")))
              .andExpect(jsonPath("$.alps.descriptors[*].id")
                         .value(not(hasItems("create-events", "update-processEngineEventDocument", "delete-processEngineEventDocument", "patch-processEngineEventDocument"))));
    }
}
