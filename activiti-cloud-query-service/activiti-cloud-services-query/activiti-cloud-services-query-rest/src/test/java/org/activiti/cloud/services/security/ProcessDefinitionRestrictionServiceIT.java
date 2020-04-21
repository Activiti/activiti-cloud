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

package org.activiti.cloud.services.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import com.querydsl.core.types.Predicate;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
public class ProcessDefinitionRestrictionServiceIT {

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ProcessDefinitionRestrictionService restrictionService;

    @MockBean
    private UserGroupManager userGroupManager;

    @MockBean
    private SecurityManager securityManager;

    private ProcessDefinitionEntity defKey1AuthorizedService;
    private ProcessDefinitionEntity defKey2AuthorizedService;
    private ProcessDefinitionEntity defKey1WildService;
    private ProcessDefinitionEntity defKey2WildService;
    private ProcessDefinitionEntity defKey3AuthorizedService;
    private ProcessDefinitionEntity defKey1UnauthorizedService;

    @BeforeEach
    public void setUp() {
        defKey1AuthorizedService = buildProcessDefinition("test-cmd-endpoint",
                                                          "defKey1");
        defKey2AuthorizedService = buildProcessDefinition("test-cmd-endpoint",
                                                          "defKey2");
        defKey3AuthorizedService = buildProcessDefinition("test-cmd-endpoint",
                                                          "defKey3");
        defKey1UnauthorizedService = buildProcessDefinition("non-authorized-service",
                                                            "defKey1");
        defKey1WildService = buildProcessDefinition("test-cmd-endpoint-wild",
                                                    "defKey1");
        defKey2WildService = buildProcessDefinition("test-cmd-endpoint-wild",
                                                    "defKey2");
        processDefinitionRepository.saveAll(Arrays.asList(defKey1AuthorizedService,
                                                          defKey2AuthorizedService,
                                                          defKey3AuthorizedService,
                                                          defKey1UnauthorizedService,
                                                          defKey1WildService,
                                                          defKey2WildService));
    }

    @AfterEach
    public void tearDown() {
        processDefinitionRepository.deleteAll();
    }

    @Test
    public void shouldGetOnlyProcessDefinitionAllowedToTheUser() {
        //given
        given(securityManager.getAuthenticatedUserId()).willReturn("testuser");

        //when
        Predicate predicate = restrictionService.restrictProcessDefinitionQuery(null,
                                                                                SecurityPolicyAccess.READ);

        //then
        Iterable<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findAll(predicate);
        assertThat(processDefinitions)
                .extracting(ProcessDefinitionEntity::getServiceName,
                            ProcessDefinitionEntity::getKey)
                .containsExactly(tuple("test-cmd-endpoint",
                                       "defKey1"));
    }

    @Test
    public void shouldGetAllDefinitionsInAllowedServiceInAdditionToDirectSpecifiedKeysWhenUsingWildcard() {
        //given
        given(securityManager.getAuthenticatedUserId()).willReturn("hruser");

        //when
        Predicate predicate = restrictionService.restrictProcessDefinitionQuery(null,
                                                                                SecurityPolicyAccess.READ);

        //then
        Iterable<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findAll(predicate);
        assertThat(processDefinitions)
                .extracting(ProcessDefinitionEntity::getServiceName,
                            ProcessDefinitionEntity::getKey)
                .containsOnly(tuple("test-cmd-endpoint",
                                    "defKey2"), // access given via key
                              tuple("test-cmd-endpoint-wild",
                                    "defKey1"), //access given via wildcard
                              tuple("test-cmd-endpoint-wild",
                                    "defKey2")); //access given via wild card
    }

    @Test
    public void shouldGetAllProcessDefinitionsAllowedToGroup() {
        //given
        when(securityManager.getAuthenticatedUserId()).thenReturn("bobinhr");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Collections.singletonList("hrgroup"));

        //when
        Predicate predicate = restrictionService.restrictProcessDefinitionQuery(null,
                                                                                SecurityPolicyAccess.READ);

        //then
        Iterable<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findAll(predicate);
        assertThat(processDefinitions)
                .extracting(ProcessDefinitionEntity::getServiceName,
                            ProcessDefinitionEntity::getKey)
                .containsOnly(tuple("test-cmd-endpoint-wild",
                                    "defKey1"), //access given via wildcard to hrgroup
                              tuple("test-cmd-endpoint-wild",
                                    "defKey2")); //access given via wildcard to hrgroup
    }

    private ProcessDefinitionEntity buildProcessDefinition(String serviceName,
                                                           String key) {
        ProcessDefinitionEntity def1 = new ProcessDefinitionEntity(serviceName,
                                                                   "full-test-cmd-endpoint",
                                                                   "v1",
                                                                   "app",
                                                                   "version");
        def1.setId(UUID.randomUUID().toString());
        def1.setKey(key);
        return def1;
    }
}
