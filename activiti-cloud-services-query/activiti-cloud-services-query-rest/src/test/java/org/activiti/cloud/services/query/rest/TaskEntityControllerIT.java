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

package org.activiti.cloud.services.query.rest;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.security.SecurityManager;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.taskFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.taskIdParameter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TaskController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.query.rest.assembler", "org.activiti.cloud.alfresco"})
public class TaskEntityControllerIT {

    private static final String TASK_ALFRESCO_IDENTIFIER = "task-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private EntityFinder entityFinder;

    @MockBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @MockBean
    private SecurityManager securityManager;

    @Test
    public void findAllShouldReturnAllResultsUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(11,
                                                                  10,
                                                                  PageRequest.of(0,
                                                                                 20));

        given(taskRepository.findAll(any(),
                                     eq(pageRequest)))
                .willReturn(new PageImpl<>(Collections.singletonList(buildDefaultTask()),
                                           pageRequest,
                                           12));

        //when
        MvcResult result = mockMvc.perform(get("/v1/tasks?skipCount=11&maxItems=10")
                                                   .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andDo(document(TASK_ALFRESCO_IDENTIFIER + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()

                ))
                .andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("list.pagination.skipCount").isEqualTo(11)
                .node("list.pagination.maxItems").isEqualTo(10)
                .node("list.pagination.count").isEqualTo(1)
                .node("list.pagination.hasMoreItems").isEqualTo(false)
                .node("list.pagination.totalItems").isEqualTo(12);
    }

    private TaskEntity buildDefaultTask() {
        return new TaskEntity(UUID.randomUUID().toString(),
                              "john",
                              "Review",
                              "Review the report",
                              new Date(),
                              new Date(),
                              20,
                              "My category",
                              UUID.randomUUID().toString(),
                              UUID.randomUUID().toString(),
                              "My app",
                              "My app",
                              "1",
                              null,
                              null,
                              Task.TaskStatus.ASSIGNED,
                              new Date(),
                              new Date(),
                              "peter",
                              null
        );
    }

    @Test
    @Ignore //@TODO: we need to update the docs that are being checked for alfresco types
    public void findByIdShouldUseAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        TaskEntity taskEntity = buildDefaultTask();
        given(entityFinder.findById(eq(taskRepository),
                                    eq(taskEntity.getId()),
                                    anyString()))
                .willReturn(taskEntity);

        Predicate restrictionPredicate = mock(Predicate.class);
        given(taskLookupRestrictionService.restrictTaskQuery(any())).willReturn(restrictionPredicate);
        given(taskRepository.findAll(restrictionPredicate)).willReturn(Collections.singletonList(taskEntity));

        //when
        this.mockMvc.perform(get("/v1/tasks/{taskId}",
                                 taskEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andDo(document(TASK_ALFRESCO_IDENTIFIER + "/get",
                                taskIdParameter(),
                                taskFields()
                       )
                );
    }
}