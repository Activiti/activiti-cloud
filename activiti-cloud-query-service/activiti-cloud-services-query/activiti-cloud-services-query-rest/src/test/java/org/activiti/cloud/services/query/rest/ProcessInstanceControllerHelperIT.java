/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.createProcessVariables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.payload.ProcessInstanceSearchRequest;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.specification.ProcessInstanceSpecification;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceHierarchyEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
import org.activiti.cloud.services.query.rest.payload.LinkProcessInstancesRequest;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "query.process-instance-link.retry.max-attempts=0",
    }
)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
@Testcontainers
@WithMockUser
class ProcessInstanceControllerHelperIT {

    private static final String TEST_USER = "testuser";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoSpyBean
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @BeforeEach
    void setUp() {
        processInstanceHierarchyRepository.deleteAll();
        processInstanceRepository.deleteAll();
    }

    @Test
    void shouldReturnAllProcessInstances() {
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        processInstanceEntity.setInitiator(TEST_USER);
        processInstanceRepository.save(processInstanceEntity);
        //given
        given(
            securityPoliciesManager.canRead(
                processInstanceEntity.getProcessDefinitionKey(),
                processInstanceEntity.getServiceName()
            )
        ).willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstances(
            predicate,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);

        ProcessInstanceEntity returnedProcessInstance = verifyReturnedProcessInstanceEntity(
            result,
            processInstanceEntity
        );

        assertThat(returnedProcessInstance).isNotNull();
        assertThat(returnedProcessInstance.getSubprocesses()).isEmpty();
    }

    @Test
    void shouldReturnAllProcessInstancesWithVariables() {
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        processInstanceEntity.setInitiator(TEST_USER);
        processInstanceRepository.save(processInstanceEntity);
        //given
        given(
            securityPoliciesManager.canRead(
                processInstanceEntity.getProcessDefinitionKey(),
                processInstanceEntity.getServiceName()
            )
        ).willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity, 8);
        List<String> variableKeys = variables.stream().map(ProcessVariableEntity::getName).toList();
        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

        Page<ProcessInstanceEntity> result = processInstanceControllerHelper.findAllProcessInstancesWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);
    }

    @Test
    void shouldReturnProcessInstanceById() {
        ProcessInstanceEntity parentProcessInstance = buildProcessInstanceEntity();
        parentProcessInstance.setInitiator(TEST_USER);
        processInstanceRepository.save(parentProcessInstance);
        ProcessInstanceEntity subprocessInstance = buildSubprocessInstance(parentProcessInstance);

        processInstanceRepository.save(parentProcessInstance);
        processInstanceRepository.save(subprocessInstance);
        //given
        given(
            securityPoliciesManager.canRead(
                parentProcessInstance.getProcessDefinitionKey(),
                parentProcessInstance.getServiceName()
            )
        ).willReturn(true);
        given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

        String processInstanceId = parentProcessInstance.getId();

        ProcessInstanceEntity result = processInstanceControllerHelper.findById(processInstanceId);

        assertThat(result).isEqualTo(parentProcessInstance);
    }

    private ProcessInstanceEntity buildSubprocessInstance(ProcessInstanceEntity parentProcessInstance) {
        ProcessInstanceEntity subprocessInstance = new ProcessInstanceEntity();
        subprocessInstance.setId(UUID.randomUUID().toString());
        subprocessInstance.setProcessDefinitionKey("mySubprocess");
        subprocessInstance.setProcessDefinitionName("subprocess");
        subprocessInstance.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        subprocessInstance.setParentId(parentProcessInstance.getId());
        return subprocessInstance;
    }

    private ProcessInstanceEntity verifyReturnedProcessInstanceEntity(
        Page<ProcessInstanceEntity> result,
        ProcessInstanceEntity parentProcessInstance
    ) {
        return result
            .getContent()
            .stream()
            .filter(pi -> pi.getId().equals(parentProcessInstance.getId()))
            .findFirst()
            .orElse(null);
    }

    @Nested
    class LinkProcessInstancesTest {

        @Test
        void shouldLinkOrphanProcessInstancesToMainProcessInstance() {
            ProcessInstanceEntity mainProcessInstance = buildProcessInstanceEntity();
            mainProcessInstance.setInitiator(TEST_USER);
            processInstanceRepository.save(mainProcessInstance);

            ProcessInstanceEntity orphanProcessInstance1 = buildProcessInstanceEntity();
            orphanProcessInstance1.setInitiator(TEST_USER);
            orphanProcessInstance1.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstance1);

            ProcessInstanceEntity orphanProcessInstance2 = buildProcessInstanceEntity();
            orphanProcessInstance2.setInitiator(TEST_USER);
            orphanProcessInstance2.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstance2);

            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

            var request = new LinkProcessInstancesRequest();
            request.setProcessInstanceIds(List.of(orphanProcessInstance1.getId(), orphanProcessInstance2.getId()));
            request.setLinkProcessInstanceType("form-type");

            processInstanceControllerHelper.linkProcessInstances(request, mainProcessInstance.getId());

            ProcessInstanceEntity updatedOrphan1 = processInstanceRepository
                .findById(orphanProcessInstance1.getId())
                .orElseThrow();
            ProcessInstanceEntity updatedOrphan2 = processInstanceRepository
                .findById(orphanProcessInstance2.getId())
                .orElseThrow();

            assertThat(updatedOrphan1.getLinkedProcessInstanceId()).isEqualTo(mainProcessInstance.getId());
            assertThat(updatedOrphan2.getLinkedProcessInstanceId()).isEqualTo(mainProcessInstance.getId());
        }

        @Test
        void shouldNotLinkProcessInstancesWithDifferentLinkType() {
            ProcessInstanceEntity mainProcessInstance = buildProcessInstanceEntity();
            mainProcessInstance.setInitiator(TEST_USER);
            processInstanceRepository.save(mainProcessInstance);

            ProcessInstanceEntity orphanProcessInstanceWithDifferentType = buildProcessInstanceEntity();
            orphanProcessInstanceWithDifferentType.setInitiator(TEST_USER);
            orphanProcessInstanceWithDifferentType.setLinkedProcessInstanceType("different-form-type");
            processInstanceRepository.save(orphanProcessInstanceWithDifferentType);

            ProcessInstanceEntity orphanProcessInstanceWithSameRequestType = buildProcessInstanceEntity();
            orphanProcessInstanceWithSameRequestType.setInitiator(TEST_USER);
            orphanProcessInstanceWithSameRequestType.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstanceWithSameRequestType);

            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

            var request = new LinkProcessInstancesRequest();
            request.setProcessInstanceIds(
                List.of(
                    orphanProcessInstanceWithDifferentType.getId(),
                    orphanProcessInstanceWithSameRequestType.getId()
                )
            );
            request.setLinkProcessInstanceType("form-type");

            processInstanceControllerHelper.linkProcessInstances(request, mainProcessInstance.getId());

            ProcessInstanceEntity updatedOrphan = processInstanceRepository
                .findById(orphanProcessInstanceWithDifferentType.getId())
                .orElseThrow();

            assertThat(updatedOrphan.getLinkedProcessInstanceId()).isNull();

            ProcessInstanceEntity updatedOrphanWithSameRequestType = processInstanceRepository
                .findById(orphanProcessInstanceWithSameRequestType.getId())
                .orElseThrow();

            assertThat(updatedOrphanWithSameRequestType.getLinkedProcessInstanceId()).isEqualTo(
                mainProcessInstance.getId()
            );
        }

        @Test
        void shouldNotLinkProcessInstancesWithDifferentMainProcessInstanceInitiator() {
            ProcessInstanceEntity mainProcessInstance = buildProcessInstanceEntity();
            mainProcessInstance.setInitiator("anotherUser");
            processInstanceRepository.save(mainProcessInstance);

            ProcessInstanceEntity orphanProcessInstance = buildProcessInstanceEntity();
            orphanProcessInstance.setInitiator(TEST_USER);
            orphanProcessInstance.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstance);

            ProcessInstanceEntity orphanProcessInstanceWithSameMainInitiator = buildProcessInstanceEntity();
            orphanProcessInstanceWithSameMainInitiator.setInitiator("anotherUser");
            orphanProcessInstanceWithSameMainInitiator.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstanceWithSameMainInitiator);

            var request = new LinkProcessInstancesRequest();
            request.setProcessInstanceIds(
                List.of(orphanProcessInstance.getId(), orphanProcessInstanceWithSameMainInitiator.getId())
            );
            request.setLinkProcessInstanceType("form-type");

            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

            processInstanceControllerHelper.linkProcessInstances(request, mainProcessInstance.getId());

            ProcessInstanceEntity orphanProcessTestUserInitiator = processInstanceRepository
                .findById(orphanProcessInstance.getId())
                .orElseThrow();

            assertThat(orphanProcessTestUserInitiator.getLinkedProcessInstanceId()).isNull();

            ProcessInstanceEntity orphanProcessMainProcessInitiator = processInstanceRepository
                .findById(orphanProcessInstanceWithSameMainInitiator.getId())
                .orElseThrow();

            assertThat(orphanProcessMainProcessInitiator.getLinkedProcessInstanceId()).isNull();
        }

        @Test
        void shouldLinkOnlyProcessInstancesWithInitiatorSameLoggedInUser() {
            ProcessInstanceEntity mainProcessInstance = buildProcessInstanceEntity();
            mainProcessInstance.setInitiator(TEST_USER);
            processInstanceRepository.save(mainProcessInstance);

            ProcessInstanceEntity orphanProcessInstance = buildProcessInstanceEntity();
            orphanProcessInstance.setInitiator("anotherUser");
            orphanProcessInstance.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstance);

            ProcessInstanceEntity orphanProcessInstanceWithSameMainInitiator = buildProcessInstanceEntity();
            orphanProcessInstanceWithSameMainInitiator.setInitiator(TEST_USER);
            orphanProcessInstanceWithSameMainInitiator.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstanceWithSameMainInitiator);

            var request = new LinkProcessInstancesRequest();
            request.setProcessInstanceIds(
                List.of(orphanProcessInstance.getId(), orphanProcessInstanceWithSameMainInitiator.getId())
            );
            request.setLinkProcessInstanceType("form-type");

            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);

            processInstanceControllerHelper.linkProcessInstances(request, mainProcessInstance.getId());

            ProcessInstanceEntity orphanProcessTestUserInitiator = processInstanceRepository
                .findById(orphanProcessInstance.getId())
                .orElseThrow();

            assertThat(orphanProcessTestUserInitiator.getLinkedProcessInstanceId()).isNull();

            ProcessInstanceEntity orphanProcessMainProcessInitiator = processInstanceRepository
                .findById(orphanProcessInstanceWithSameMainInitiator.getId())
                .orElseThrow();

            assertThat(orphanProcessMainProcessInitiator.getLinkedProcessInstanceId()).isNotNull();
        }

        @Test
        void should_notLinkAndNotThrow_when_mainProcessInstanceIdIsInvalid() {
            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
            ProcessInstanceEntity orphanProcessInstance = buildProcessInstanceEntity();
            orphanProcessInstance.setInitiator(TEST_USER);
            orphanProcessInstance.setLinkedProcessInstanceType("form-type");
            processInstanceRepository.save(orphanProcessInstance);

            var request = new LinkProcessInstancesRequest();
            request.setProcessInstanceIds(List.of(orphanProcessInstance.getId()));
            request.setLinkProcessInstanceType("form-type");

            String invalidMainProcessInstanceId = UUID.randomUUID().toString();

            processInstanceControllerHelper.linkProcessInstances(request, invalidMainProcessInstanceId);

            assertThat(
                processInstanceRepository
                    .findById(orphanProcessInstance.getId())
                    .orElseThrow()
                    .getLinkedProcessInstanceId()
            ).isNull();
        }

        @Test
        void should_registerHierarchyAndCountRelatedProcesses_when_orphanWithSubtreeIsLinked() {
            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
            ProcessInstanceEntity main = saveProcess(null);
            ProcessInstanceEntity orphan = saveProcess("form-type");
            ProcessInstanceEntity child = saveProcess(null);
            saveSelfRow(main);
            saveSelfRow(orphan);
            saveSelfRow(child);
            saveHierarchyRow(orphan, child, 1, ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS);

            link(main, orphan);

            assertThat(processInstanceHierarchyRepository.findByDescendantId(child.getId()))
                .extracting(
                    ProcessInstanceHierarchyEntity::getAncestorId,
                    ProcessInstanceHierarchyEntity::getDepth,
                    ProcessInstanceHierarchyEntity::getRelationType
                )
                .containsExactlyInAnyOrder(
                    tuple(child.getId(), 0, ProcessInstanceHierarchyEntity.RELATION_SELF),
                    tuple(orphan.getId(), 1, ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS),
                    tuple(main.getId(), 2, ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS)
                );
            assertThat(relatedTo(main)).containsExactlyInAnyOrder(main.getId(), orphan.getId(), child.getId());
            assertThat(relatedCounts(main))
                .containsEntry(ProcessInstanceHierarchyEntity.RELATION_LINKED, 1L)
                .containsEntry(ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS, 1L);
        }

        @Test
        void should_replaceLongerHierarchyRow_when_childIsLinkedInLaterRequest() {
            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
            ProcessInstanceEntity main = saveProcess(null);
            ProcessInstanceEntity parent = saveProcess("form-type");
            ProcessInstanceEntity child = saveProcess("form-type");
            saveSelfRow(main);
            saveSelfRow(parent);
            saveSelfRow(child);
            saveHierarchyRow(parent, child, 1, ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS);

            link(main, parent);
            link(main, child);

            assertThat(processInstanceHierarchyRepository.findByDescendantId(child.getId()))
                .extracting(
                    ProcessInstanceHierarchyEntity::getAncestorId,
                    ProcessInstanceHierarchyEntity::getDepth,
                    ProcessInstanceHierarchyEntity::getRelationType
                )
                .contains(tuple(main.getId(), 1, ProcessInstanceHierarchyEntity.RELATION_LINKED));
            assertThat(relatedCounts(main))
                .containsEntry(ProcessInstanceHierarchyEntity.RELATION_LINKED, 2L)
                .doesNotContainKey(ProcessInstanceHierarchyEntity.RELATION_SUBPROCESS);
        }

        @Test
        void should_rollbackLinkAndHierarchyTogether_when_hierarchyRegistrationFails() {
            given(securityManager.getAuthenticatedUserId()).willReturn(TEST_USER);
            ProcessInstanceEntity main = saveProcess(null);
            ProcessInstanceEntity orphan = saveProcess("form-type");
            saveSelfRow(main);
            saveSelfRow(orphan);
            doThrow(new IllegalStateException("hierarchy failure"))
                .when(processInstanceHierarchyRepository)
                .saveAll(any());

            link(main, orphan);

            assertThat(
                processInstanceRepository.findById(orphan.getId()).orElseThrow().getLinkedProcessInstanceId()
            ).isNull();
            assertThat(processInstanceHierarchyRepository.findByDescendantId(orphan.getId()))
                .extracting(ProcessInstanceHierarchyEntity::getAncestorId)
                .containsExactly(orphan.getId());
        }

        private ProcessInstanceEntity saveProcess(String linkType) {
            ProcessInstanceEntity entity = buildProcessInstanceEntity();
            entity.setInitiator(TEST_USER);
            entity.setLinkedProcessInstanceType(linkType);
            return processInstanceRepository.save(entity);
        }

        private void saveSelfRow(ProcessInstanceEntity process) {
            saveHierarchyRow(process, process, 0, ProcessInstanceHierarchyEntity.RELATION_SELF);
        }

        private void saveHierarchyRow(
            ProcessInstanceEntity ancestor,
            ProcessInstanceEntity descendant,
            int depth,
            String relationType
        ) {
            processInstanceHierarchyRepository.save(
                new ProcessInstanceHierarchyEntity(ancestor.getId(), descendant.getId(), depth, relationType)
            );
        }

        private void link(ProcessInstanceEntity main, ProcessInstanceEntity orphan) {
            var request = new LinkProcessInstancesRequest();
            request.setProcessInstanceIds(List.of(orphan.getId()));
            request.setLinkProcessInstanceType("form-type");
            processInstanceControllerHelper.linkProcessInstances(request, main.getId());
        }

        private Set<String> relatedTo(ProcessInstanceEntity process) {
            var searchRequest = new ProcessInstanceSearchRequest();
            searchRequest.setProcessRelatedTo(Set.of(process.getId()));
            return processInstanceRepository
                .findAll(ProcessInstanceSpecification.unrestricted(searchRequest))
                .stream()
                .map(ProcessInstanceEntity::getId)
                .collect(Collectors.toSet());
        }

        private Map<String, Long> relatedCounts(ProcessInstanceEntity process) {
            return processInstanceHierarchyRepository
                .countRelatedByAncestor(List.of(process.getId()))
                .stream()
                .collect(
                    Collectors.toMap(
                        ProcessInstanceHierarchyRepository.RelatedProcessCountProjection::getRelationType,
                        ProcessInstanceHierarchyRepository.RelatedProcessCountProjection::getRelatedCount
                    )
                );
        }
    }
}
