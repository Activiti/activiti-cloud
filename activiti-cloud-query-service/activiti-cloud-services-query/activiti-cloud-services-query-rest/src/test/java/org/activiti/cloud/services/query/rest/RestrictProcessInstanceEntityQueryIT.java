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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.querydsl.core.types.Predicate;
import java.util.Collections;
import java.util.Iterator;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.security.ProcessInstanceRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@EnableAutoConfiguration
public class RestrictProcessInstanceEntityQueryIT {

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private ProcessInstanceRestrictionService processInstanceRestrictionService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;

    @BeforeEach
    public void setUp() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("15");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("testuser");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);
    }

    @Test
    public void shouldGetProcessInstancesWhenPermitted() {
        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            null,
            SecurityPolicyAccess.READ
        );
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetProcessInstancesWhenUserPermittedByWildcard() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("16");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("hruser");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            null,
            SecurityPolicyAccess.READ
        );
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldGetProcessInstancesWhenGroupPermittedByWildcard() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("17");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("bobinhr");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("bobinhr");
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Collections.singletonList("hrgroup"));

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            null,
            SecurityPolicyAccess.READ
        );
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetProcessInstancesWhenPolicyNotForUser() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("18");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("testuser");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            null,
            SecurityPolicyAccess.READ
        );
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        //this user should see proc instances - but not for test-cmd-endpoint-wild

        Iterator<ProcessInstanceEntity> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            ProcessInstanceEntity proc = iterator.next();
            assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-wild");
            assertThat(proc.getServiceName()).isEqualToIgnoringCase("test-cmd-endpoint");
        }
    }

    @Test
    public void shouldMatchAppNameCaseInsensitiveIgnoringHyphens() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("19");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("testuser");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("Te-St-CmD-EnDpoInT");
        processInstanceRepository.save(processInstanceEntity);

        ProcessInstanceEntity processInstanceEntity2 = new ProcessInstanceEntity();
        processInstanceEntity2.setId("20");
        processInstanceEntity2.setName("name");
        processInstanceEntity2.setInitiator("testuser");
        processInstanceEntity2.setProcessDefinitionKey("defKey1");
        processInstanceEntity2.setServiceName("test-cmd-endpoint-dontmatchthisone");
        processInstanceRepository.save(processInstanceEntity2);

        assertThat(processInstanceRepository.count()).isGreaterThanOrEqualTo(2);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            null,
            SecurityPolicyAccess.READ
        );
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);

        Iterator<ProcessInstanceEntity> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            ProcessInstanceEntity proc = iterator.next();
            assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-dontmatchthisone");
            assertThat(proc.getServiceName().replace("-", ""))
                .isEqualToIgnoringCase("test-cmd-endpoint".replace("-", ""));
        }

        assertThat(processInstanceRepository.count(predicate)).isEqualTo(2);
    }

    @Test
    public void shouldNotGetProcessInstancesWhenNotPermitted() {
        Predicate predicate = QProcessInstanceEntity.processInstanceEntity.id.isNotNull();

        when(securityManager.getAuthenticatedUserId()).thenReturn("intruder");

        predicate =
            processInstanceRestrictionService.restrictProcessInstanceQuery(predicate, SecurityPolicyAccess.READ);
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }

    @Test
    public void shouldGetProcessInstancesWhenMatchesFullServiceName() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("21");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("hruser");
        processInstanceEntity.setProcessDefinitionKey("defKey2");
        processInstanceEntity.setServiceFullName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");

        Predicate predicate = processInstanceRestrictionService.restrictProcessInstanceQuery(
            null,
            SecurityPolicyAccess.READ
        );
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAll(predicate);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }
}
