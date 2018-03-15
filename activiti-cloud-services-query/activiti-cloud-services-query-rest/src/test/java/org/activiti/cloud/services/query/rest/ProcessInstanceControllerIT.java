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
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstance;
import org.activiti.cloud.services.security.AuthenticationWrapper;
import org.activiti.cloud.services.security.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processInstanceFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processInstanceIdParameter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceController.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.query.rest.assembler", "org.activiti.cloud.alfresco"})
public class ProcessInstanceControllerIT {

    private static final String PROCESS_INSTANCE_ALFRESCO_IDENTIFIER = "process-instance-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockBean
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @MockBean
    private AuthenticationWrapper authenticationWrapper;

    @MockBean
    private EntityFinder entityFinder;

    @Before
    public void setUp() throws Exception {
        when(authenticationWrapper.getAuthenticatedUserId()).thenReturn("user");
    }

    @Test
    public void findAllShouldReturnAllResultsUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        Predicate restrictedPredicate = mock(Predicate.class);
        given(securityPoliciesApplicationService.restrictProcessInstanceQuery(any(),
                                                                              eq(SecurityPolicy.READ))).willReturn(restrictedPredicate);
        given(processInstanceRepository.findAll(eq(restrictedPredicate),
                                                ArgumentMatchers.<Pageable>any())).willReturn(new PageImpl<>(Collections.singletonList(buildDefaultProcessInstance()),
                                                                                                             PageRequest.of(1,
                                                                                                                            10),
                                                                                                             11));


        //when
        mockMvc.perform(get("/v1/process-instances?skipCount=10&maxItems=10")
                                .accept(MediaType.APPLICATION_JSON))
                //then
                .andExpect(status().isOk())
                .andDo(document(PROCESS_INSTANCE_ALFRESCO_IDENTIFIER + "/list",
                                pageRequestParameters(),
                                pagedResourcesResponseFields()

                ));
    }


    private ProcessInstance buildDefaultProcessInstance() {
        return new ProcessInstance("My-app",
                                   UUID.randomUUID().toString(),
                                   UUID.randomUUID().toString(),
                                   org.activiti.cloud.services.api.model.ProcessInstance.ProcessInstanceStatus.RUNNING.name(),
                                   new Date());
    }

    @Test
    public void findByIdShouldUseAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        ProcessInstance processInstance = buildDefaultProcessInstance();
        given(entityFinder.findById(eq(processInstanceRepository), eq(processInstance.getId()), any())).willReturn(processInstance);
        given(securityPoliciesApplicationService.canRead(processInstance.getProcessDefinitionKey(), processInstance.getApplicationName()))
                .willReturn(true);

        //when
        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                                    processInstance.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andDo(document(PROCESS_INSTANCE_ALFRESCO_IDENTIFIER + "/get",
                                processInstanceIdParameter(),
                                processInstanceFields()
                       )
                );

    }

}