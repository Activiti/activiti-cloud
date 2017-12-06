package org.activiti.cloud.services.organization.rest.api;

import org.activiti.cloud.organization.core.model.Model;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ModelRestTest extends AbstractRestTest {
    @Test
    public void getModels() throws Exception {
        mockMvc.perform(get("/models/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(1)));
    }

    @Test
    public void getModelById() throws Exception {
        mockMvc.perform(get("/models/{modelId}",
                            PROCESS_MODEL_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type",
                                    is(Model.ModelType.PROCESS_MODEL.toString())));

    }
}
