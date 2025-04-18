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

package org.activiti.cloud.services.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.introproventures.graphql.jpa.query.schema.RestrictedKeysProvider;
import com.introproventures.graphql.jpa.query.schema.impl.EntityIntrospector;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
public class ActivitiRestrictedKeysSupplierProviderTest {

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private RestrictedKeysProvider restrictedKeysProvider;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private UserGroupManager userGroupManager;

    @MockitoSpyBean
    private SecurityManager securityManager;

    private ProcessDefinitionEntity defKey1AuthorizedService;
    private ProcessDefinitionEntity defKey2AuthorizedService;
    private ProcessDefinitionEntity defKey1WildService;
    private ProcessDefinitionEntity defKey2WildService;
    private ProcessDefinitionEntity defKey3AuthorizedService;
    private ProcessDefinitionEntity defKey1UnauthorizedService;

    @BeforeEach
    public void setUp() {
        defKey1AuthorizedService = buildProcessDefinition("test-cmd-endpoint", "defKey1");
        defKey2AuthorizedService = buildProcessDefinition("test-cmd-endpoint", "defKey2");
        defKey3AuthorizedService = buildProcessDefinition("test-cmd-endpoint", "defKey3");
        defKey1UnauthorizedService = buildProcessDefinition("non-authorized-service", "defKey1");
        defKey1WildService = buildProcessDefinition("test-cmd-endpoint-wild", "defKey1");
        defKey2WildService = buildProcessDefinition("test-cmd-endpoint-wild", "defKey2");
        processDefinitionRepository.saveAll(
            Arrays.asList(
                defKey1AuthorizedService,
                defKey2AuthorizedService,
                defKey3AuthorizedService,
                defKey1UnauthorizedService,
                defKey1WildService,
                defKey2WildService
            )
        );

        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("15");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("testuser");
        processInstanceEntity.setProcessDefinitionKey("defKey1");
        processInstanceEntity.setServiceName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        taskCandidateUserRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        processDefinitionRepository.deleteAll();
    }

    @Test
    @WithMockUser("testuser")
    public void shouldGetOnlyProcessDefinitionAllowedToTheUser() {
        //given
        var entityDescriptor = introspect(ProcessDefinitionEntity.class);

        //when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        //then
        Iterable<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findAllById(
            toIterable(result)
        );
        assertThat(processDefinitions)
            .extracting(ProcessDefinitionEntity::getServiceName, ProcessDefinitionEntity::getKey)
            .containsExactly(tuple("test-cmd-endpoint", "defKey1"));
    }

    @Test
    @WithMockUser("hruser")
    public void shouldGetAllDefinitionsInAllowedServiceInAdditionToDirectSpecifiedKeysWhenUsingWildcard() {
        //given
        var entityDescriptor = introspect(ProcessDefinitionEntity.class);

        //when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        //then
        Iterable<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findAllById(
            toIterable(result)
        );

        assertThat(processDefinitions)
            .extracting(ProcessDefinitionEntity::getServiceName, ProcessDefinitionEntity::getKey)
            .containsOnly(
                tuple("test-cmd-endpoint", "defKey2"), // access given via key
                tuple("test-cmd-endpoint-wild", "defKey1"), //access given via wildcard
                tuple("test-cmd-endpoint-wild", "defKey2")
            ); //access given via wild card
    }

    @Test
    @WithMockUser("bobinhr")
    public void shouldGetAllProcessDefinitionsAllowedToGroup() {
        //given
        var entityDescriptor = introspect(ProcessDefinitionEntity.class);
        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Collections.singletonList("hrgroup"));

        //when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        //then
        Iterable<ProcessDefinitionEntity> processDefinitions = processDefinitionRepository.findAllById(
            toIterable(result)
        );

        assertThat(processDefinitions)
            .extracting(ProcessDefinitionEntity::getServiceName, ProcessDefinitionEntity::getKey)
            .containsOnly(
                tuple("test-cmd-endpoint-wild", "defKey1"), //access given via wildcard to hrgroup
                tuple("test-cmd-endpoint-wild", "defKey2")
            ); //access given via wildcard to hrgroup
    }

    @Test
    void contextLoads() {
        assertThat(restrictedKeysProvider).isInstanceOf(ActivitiRestrictedKeysProvider.class);
    }

    @Test
    @WithMockUser("otheruser")
    void shouldReturnEmptyProcessDefinitionEntityRestrictedKeys() {
        // given
        var entityDescriptor = introspect(ProcessDefinitionEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser("otheruser")
    void processInstanceEntityRestrictedKeys() {
        // given
        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser("otheruser")
    void taskEntityRestrictedKeys() {
        // given
        var entityDescriptor = introspect(TaskEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser("otheruser")
    void processVariableRestrictedKeys() {
        // given
        var entityDescriptor = introspect(ProcessVariableEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser("otheruser")
    void taskVariableRestrictedKeys() {
        // given
        var entityDescriptor = introspect(TaskVariableEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser(username = "otheruser")
    void serviceTaskRestrictedKeys() {
        var entityDescriptor = introspect(ServiceTaskEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ACTIVITI_ADMIN")
    void unrestrictedKeys() {
        var entityDescriptor = introspect(ProcessDefinitionEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isNotEmpty().get().isEqualTo(List.of("*"));
    }

    @Test
    @WithAnonymousUser
    void anonymousUserKeys() {
        var entityDescriptor = introspect(ProcessDefinitionEntity.class);

        // when
        var result = catchThrowable(() -> restrictedKeysProvider.apply(entityDescriptor));

        // then
        assertThat(result).isInstanceOf(AccessDeniedException.class).hasMessage("Access denied");
    }

    @Test
    @WithMockUser("testuser")
    public void shouldGetProcessInstancesWhenPermitted() {
        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAllById(toIterable(result));

        assertThat(iterable).isNotEmpty();
    }

    @Test
    @WithMockUser("hruser")
    public void shouldGetProcessInstancesWhenUserPermittedByWildcard() {
        //given
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("16");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("hruser");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAllById(toIterable(result));

        assertThat(iterable).isNotEmpty();
    }

    @Test
    @WithMockUser("bobinhr")
    public void shouldGetProcessInstancesWhenGroupPermittedByWildcard() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("17");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("bobinhr");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Collections.singletonList("hrgroup"));

        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAllById(toIterable(result));

        assertThat(iterable).isNotEmpty();
    }

    @Test
    @WithMockUser("testuser")
    public void shouldNotGetProcessInstancesWhenPolicyNotForUser() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("18");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("testuser");
        processInstanceEntity.setProcessDefinitionKey("defKeyWild");
        processInstanceEntity.setServiceName("test-cmd-endpoint-wild");
        processInstanceRepository.save(processInstanceEntity);

        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAllById(toIterable(result));

        assertThat(iterable)
            .isNotEmpty()
            //this user should see proc instances - but not for test-cmd-endpoint-wild
            .allSatisfy(proc -> {
                assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-wild");
                assertThat(proc.getServiceName()).isEqualToIgnoringCase("test-cmd-endpoint");
            });
    }

    @Test
    @WithMockUser("testuser")
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

        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAllById(toIterable(result));

        assertThat(iterable)
            .isNotEmpty()
            .allSatisfy(proc -> {
                assertThat(proc.getServiceName()).isNotEqualToIgnoringCase("test-cmd-endpoint-dontmatchthisone");
                assertThat(proc.getServiceName().replace("-", ""))
                    .isEqualToIgnoringCase("test-cmd-endpoint".replace("-", ""));
            });

        assertThat(processInstanceRepository.findAllById(toIterable(result))).hasSize(2);
    }

    @Test
    @WithMockUser("intruder")
    public void shouldNotGetProcessInstancesWhenNotPermitted() {
        // given
        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser("hruser")
    public void shouldGetProcessInstancesWhenMatchesFullServiceName() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("21");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("hruser");
        processInstanceEntity.setProcessDefinitionKey("defKey2");
        processInstanceEntity.setServiceFullName("test-cmd-endpoint");
        processInstanceRepository.save(processInstanceEntity);

        // given
        var entityDescriptor = introspect(ProcessInstanceEntity.class);

        // when
        var result = restrictedKeysProvider.apply(entityDescriptor);

        // then
        Iterable<ProcessInstanceEntity> iterable = processInstanceRepository.findAllById(toIterable(result));
    }

    @Test
    @WithMockUser("testuser")
    void shouldGetTasksWhenCandidate() {
        TaskEntity taskEntity = new TaskEntity();
        String taskId = UUID.randomUUID().toString();
        taskEntity.setId(taskId);
        taskRepository.save(taskEntity);

        TaskCandidateUserEntity taskCandidateUser = new TaskCandidateUserEntity(taskEntity.getId(), "testuser");
        taskCandidateUserRepository.save(taskCandidateUser);

        when(securityManager.getAuthenticatedUserGroups()).thenReturn(Arrays.asList("testgroup"));
        shouldGetTasksWhenCandidateRestrictTaskQuery();
        shouldGetTasksWhenCandidateRestrictToInvolvedUser();
    }

    private void shouldGetTasksWhenCandidateRestrictTaskQuery() {
        var entityDescriptor = introspect(TaskEntity.class);

        var restrictedKeys = restrictedKeysProvider.apply(entityDescriptor);

        assertThat(restrictedKeys).isNotEmpty().get().asList().isNotEmpty();

        Iterable<TaskEntity> iterable = taskRepository.findAllById(toIterable(restrictedKeys));

        assertThat(iterable).isNotEmpty();
    }

    private void shouldGetTasksWhenCandidateRestrictToInvolvedUser() {
        var entityDescriptor = introspect(TaskEntity.class);

        var restrictedKeys = restrictedKeysProvider.apply(entityDescriptor);

        assertThat(restrictedKeys).isNotEmpty().get().asList().isNotEmpty();
    }

    private ProcessDefinitionEntity buildProcessDefinition(String serviceName, String key) {
        ProcessDefinitionEntity def1 = new ProcessDefinitionEntity(
            serviceName,
            "full-test-cmd-endpoint",
            "v1",
            "app",
            "version"
        );
        def1.setId(UUID.randomUUID().toString());
        def1.setKey(key);
        return def1;
    }

    private EntityIntrospector.EntityIntrospectionResult introspect(Class<?> entityClass) {
        return EntityIntrospector.introspect(entityManager.getMetamodel().entity(entityClass));
    }

    private <ID> Iterable<ID> toIterable(Optional<List<Object>> list) {
        return list.get().stream().map(it -> (ID) it).toList();
    }
}
