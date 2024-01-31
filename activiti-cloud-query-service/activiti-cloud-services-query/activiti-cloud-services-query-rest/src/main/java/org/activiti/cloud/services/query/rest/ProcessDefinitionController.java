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

import static org.activiti.cloud.services.query.rest.RestDocConstants.PREDICATE_DESC;
import static org.activiti.cloud.services.query.rest.RestDocConstants.PREDICATE_EXAMPLE;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedModelAssembler;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.QProcessDefinitionEntity;
import org.activiti.cloud.services.query.rest.assembler.ProcessDefinitionRepresentationModelAssembler;
import org.activiti.cloud.services.security.ProcessDefinitionRestrictionService;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ExposesResourceFor(ProcessDefinitionEntity.class)
@RequestMapping(
    value = "/v1/process-definitions",
    produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
)
@Tag(name = "Process Definition Controller")
public class ProcessDefinitionController {

    private static final String EVERYONE_GROUP = "*";

    private ProcessDefinitionRepository repository;

    private AlfrescoPagedModelAssembler<ProcessDefinitionEntity> pagedCollectionModelAssembler;

    private ProcessDefinitionRepresentationModelAssembler processDefinitionRepresentationModelAssembler;

    private ProcessDefinitionRestrictionService processDefinitionRestrictionService;
    private SecurityManager securityManager;

    public ProcessDefinitionController(
        ProcessDefinitionRepository repository,
        AlfrescoPagedModelAssembler<ProcessDefinitionEntity> pagedCollectionModelAssembler,
        ProcessDefinitionRepresentationModelAssembler processDefinitionRepresentationModelAssembler,
        ProcessDefinitionRestrictionService processDefinitionRestrictionService,
        SecurityManager securityManager
    ) {
        this.repository = repository;
        this.pagedCollectionModelAssembler = pagedCollectionModelAssembler;
        this.processDefinitionRepresentationModelAssembler = processDefinitionRepresentationModelAssembler;
        this.processDefinitionRestrictionService = processDefinitionRestrictionService;
        this.securityManager = securityManager;
    }

    @GetMapping
    public PagedModel<EntityModel<CloudProcessDefinition>> findAllProcess(
        @Parameter(description = PREDICATE_DESC, example = PREDICATE_EXAMPLE) @QuerydslPredicate(
            root = ProcessDefinitionEntity.class
        ) Predicate predicate,
        Pageable pageable
    ) {
        Predicate predicateRestricted = applyRestrictions(predicate);
        return pagedCollectionModelAssembler.toModel(
            pageable,
            repository.findAll(predicateRestricted, pageable),
            processDefinitionRepresentationModelAssembler
        );
    }

    private Predicate applyRestrictions(Predicate predicate) {
        Predicate extendedPredicate = processDefinitionRestrictionService.restrictProcessDefinitionQuery(
            Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new),
            SecurityPolicyAccess.READ
        );

        String userId = securityManager.getAuthenticatedUserId();
        BooleanExpression candidateStarterExpression = QProcessDefinitionEntity.processDefinitionEntity.candidateStarterUsers
            .any()
            .userId.eq(userId);

        List<String> groupIds = getCurrentUserGroupsIncludingEveryOneGroup();
        if (!groupIds.isEmpty()) {
            candidateStarterExpression =
                candidateStarterExpression.or(
                    QProcessDefinitionEntity.processDefinitionEntity.candidateStarterGroups.any().groupId.in(groupIds)
                );
        }

        return candidateStarterExpression.and(extendedPredicate);
    }

    private List<String> getCurrentUserGroupsIncludingEveryOneGroup() {
        List<String> groups = new ArrayList<>(securityManager.getAuthenticatedUserGroups());
        groups.add(EVERYONE_GROUP);
        return groups;
    }
}
