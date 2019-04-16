package org.activiti.cloud.starter.audit.tests.it.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.nio.charset.StandardCharsets;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuditSwaggerITSupport {

    @Autowired
    private WebApplicationContext context;

    private Gson formatter = new GsonBuilder().setPrettyPrinting().create();

    private JsonParser parser = new JsonParser();

    /**
     * This is more than a simple test. It's actually generating the swagger.json definition of the service
     */
    @Test
    public void generateSwagger() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/api-docs").accept(MediaType.APPLICATION_JSON))
                .andDo((result) -> {
                    JsonElement je = parser.parse(result.getResponse().getContentAsString());
                    FileUtils.writeStringToFile(new File("target/swagger.json"),
                            formatter.toJson(je),
                            StandardCharsets.UTF_8);
                    JsonNode jsonNodeTree = new ObjectMapper().readTree(result.getResponse().getContentAsString());
                    FileUtils.writeStringToFile(new File("target/swagger.yaml"),
                            new YAMLMapper().writeValueAsString(jsonNodeTree),
                            StandardCharsets.UTF_8);
                });
        mockMvc.perform(MockMvcRequestBuilders.get("/v2/api-docs?group=hal").accept(MediaType.APPLICATION_JSON))
                .andDo((result) -> {
                    JsonElement je = parser.parse(result.getResponse().getContentAsString());
                    FileUtils.writeStringToFile(new File("target/swagger-hal.json"),
                            formatter.toJson(je),
                            StandardCharsets.UTF_8);
                    // TODO: 09/04/2019 the yaml generated out this json file will be produced once we make sure clients can be generated with swagger-hal.json
                });
    }
}
