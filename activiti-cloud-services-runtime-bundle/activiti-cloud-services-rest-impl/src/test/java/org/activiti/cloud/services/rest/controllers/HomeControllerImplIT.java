/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.rest.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(HomeControllerImpl.class)
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class HomeControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "home";

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getHomeInfo() throws Exception {
        this.mockMvc.perform(get("/v1/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome to an instance of the Activiti Process Engine")))
                .andDo(document(DOCUMENTATION_IDENTIFIER,
                                links(halLinks(),
                                        linkWithRel("process-definitions").description("The process-definitions"),
                                        linkWithRel("process-instances").description("The process-instances"),
                                        linkWithRel("tasks").description("Tasks")),
                                responseFields(
                                        fieldWithPath("welcome").type(JsonFieldType.STRING).description("The welcome message"),
                                        subsectionWithPath("_links").description("Other resources"))));
    }
}