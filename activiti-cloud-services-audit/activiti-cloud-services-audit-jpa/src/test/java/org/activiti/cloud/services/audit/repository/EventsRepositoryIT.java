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

package org.activiti.cloud.services.audit.repository;

import org.activiti.cloud.services.audit.events.ActivityStartedEventEntity;
import org.activiti.cloud.services.audit.events.ProcessEngineEventEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.cloud.services.audit.config.RepositoryConfig.API_VERSION;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class EventsRepositoryIT {

    private static final String DOCUMENTATION_IDENTIFIER = "events";

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getEvents() throws Exception {
        mockMvc.perform(get("{version}/events",
                            API_VERSION)
                                .param("page",
                                       "0")
                                .param("size",
                                       "25")
                                .param("sort",
                                       "asc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(
                                        subsectionWithPath("_embedded.events").description("A list of events "),
                                        subsectionWithPath("_links.self").description("Resource Self Link"),
                                        subsectionWithPath("_links.profile").description("Resource Profile Link"),
                                        subsectionWithPath("page").description("Pagination details."))));
    }

    @Test
    public void headEvents() throws Exception {
        mockMvc.perform(head("{version}/events",
                             API_VERSION))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/head/list"));
    }

    @Test
    public void getEventById() throws Exception {
        ProcessEngineEventEntity event = new ActivityStartedEventEntity();
        final ProcessEngineEventEntity activitiStartedEvent = eventsRepository.save(event);
        mockMvc.perform(get("{version}/events/{id}",
                            API_VERSION,
                            activitiStartedEvent.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("id").description("The event id"),
                                               parameterWithName("version").description("The API version")),
                                responseFields(
                                        subsectionWithPath("id").description("The event id"),
                                        subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                                )));
    }

    @Test
    public void headEventById() throws Exception {
        ProcessEngineEventEntity event = new ActivityStartedEventEntity();
        final ProcessEngineEventEntity activitiStartedEvent = eventsRepository.save(event);
        mockMvc.perform(head("{version}/events/{id}",
                             API_VERSION,
                             activitiStartedEvent.getId()))
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/head"));
    }
}
