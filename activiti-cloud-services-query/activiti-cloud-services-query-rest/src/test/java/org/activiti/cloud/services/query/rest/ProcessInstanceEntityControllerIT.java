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

import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pageRequestParameters;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.pagedResourcesResponseFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processInstanceFields;
import static org.activiti.alfresco.rest.docs.AlfrescoDocumentation.processInstanceIdParameter;
import static org.activiti.alfresco.rest.docs.HALDocumentation.pageLinks;
import static org.activiti.alfresco.rest.docs.HALDocumentation.pagedProcessInstanceFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.EntityFinder;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.cloud.services.security.TaskLookupRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceController.class)
@Import({
        QueryRestWebMvcAutoConfiguration.class,
        CommonModelAutoConfiguration.class,
        AlfrescoWebAutoConfiguration.class
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class ProcessInstanceEntityControllerIT {

    private static final String PROCESS_INSTANCE_ALFRESCO_IDENTIFIER = "process-instance-alfresco";
    private static final String PROCESS_INSTANCE_IDENTIFIER = "process-instance";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockBean
    private SecurityPoliciesManager securityPoliciesApplicationService;

    @MockBean
    private ProcessInstanceRestrictionService processInstanceRestrictionService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private EntityFinder entityFinder;

    @MockBean
    private ProcessDefinitionRepository processDefinitionRepository;

    @MockBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockBean
    private TaskLookupRestrictionService taskLookupRestrictionService;

    @Before
    public void setUp() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("user");
        assertThat(processDefinitionRepository).isNotNull();
        assertThat(securityPoliciesProperties).isNotNull();
        assertThat(taskLookupRestrictionService).isNotNull();
    }

    @Test
    public void findAllShouldReturnAllResultsUsingAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        Predicate restrictedPredicate = mock(Predicate.class);
        given(processInstanceRestrictionService.restrictProcessInstanceQuery(any(),
                                                                              eq(SecurityPolicyAccess.READ))).willReturn(restrictedPredicate);
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

    @Test
    public void findAllShouldReturnAllResultsUsingHalWhenMediaTypeIsApplicationHalJson() throws Exception {
        //given
        Predicate restrictedPredicate = mock(Predicate.class);
        given(processInstanceRestrictionService.restrictProcessInstanceQuery(any(),
                                                                              eq(SecurityPolicyAccess.READ))).willReturn(restrictedPredicate);
        given(processInstanceRepository.findAll(eq(restrictedPredicate),
                                                ArgumentMatchers.<Pageable>any())).willReturn(new PageImpl<>(Collections.singletonList(buildDefaultProcessInstance()),
                                                                                                             PageRequest.of(1,
                                                                                                                            10),
                                                                                                             11));


        //when
        mockMvc.perform(get("/v1/process-instances?page=1&size=10")
                                .accept(MediaTypes.HAL_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andDo(document(PROCESS_INSTANCE_IDENTIFIER + "/list",
                                pageLinks(),
                                pagedProcessInstanceFields()
                ));
    }


    private ProcessInstanceEntity buildDefaultProcessInstance() {
        return new ProcessInstanceEntity("My-app", "My-app", "1", null, null,
                                         UUID.randomUUID().toString(),
                                         UUID.randomUUID().toString(),
                                         ProcessInstance.ProcessInstanceStatus.RUNNING,
                                         new Date());
    }

    @Test
    public void findByIdShouldUseAlfrescoMetadataWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        ProcessInstanceEntity processInstanceEntity = buildDefaultProcessInstance();
        given(entityFinder.findById(eq(processInstanceRepository), eq(processInstanceEntity.getId()), any())).willReturn(processInstanceEntity);
        given(securityPoliciesApplicationService.canRead(processInstanceEntity.getProcessDefinitionKey(), processInstanceEntity.getServiceName()))
                .willReturn(true);

        //when
        this.mockMvc.perform(get("/v1/process-instances/{processInstanceId}",
                                 processInstanceEntity.getId()).accept(MediaType.APPLICATION_JSON_VALUE))
                //then
                .andExpect(status().isOk())
                .andDo(document(PROCESS_INSTANCE_ALFRESCO_IDENTIFIER + "/get",
                                processInstanceIdParameter(),
                                processInstanceFields()
                       )
                );

    }

}