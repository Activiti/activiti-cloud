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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableFilterType;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.ProcessVariableValueFilter;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.predicate.QueryDslPredicateFilter;
import org.activiti.cloud.services.query.rest.predicate.RootTasksFilter;
import org.activiti.cloud.services.query.rest.predicate.StandAloneTaskFilter;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
//TODO Make the test work using AlfrescoJackson2HttpMessageConverter to simulate the actual response that we have in real environment
public class TaskControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine").withReuse(true);

    @BeforeEach
    public void setUp() {
        webAppContextSetup(context);
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariableExactQuery() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final String varName = "var-to-search";
        final String varValue = "value-to-search";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(varValue);
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(varValue);
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue("other-value");
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "string",
                varValue,
                ProcessVariableFilterType.EQUALS
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(varValue))
            );
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariableContainsQuery() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final String varName = "var-to-search";
        final String varValue = "Johnny B. Goode";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(varValue);
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        ProcessVariableEntity otherProcessVar = new ProcessVariableEntity();
        otherProcessVar.setName("other-var");
        otherProcessVar.setValue("other-value");
        otherProcessVar.setProcessInstanceId(processInstance1.getId());
        otherProcessVar.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        otherProcessVar.setProcessInstance(processInstance1);
        variableRepository.save(otherProcessVar);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(varValue);
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue("other-value");
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1, otherProcessVar), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        String searchParam = varValue.substring(2, 6);
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "string",
                searchParam,
                ProcessVariableFilterType.CONTAINS
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && ((String) pv.getValue()).contains(searchParam))
            );
    }

    @Test
    public void should_returnTasks_filteredByIntegerProcessVariableExactQuery() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final String varName = "var-to-search";
        final Integer varValue = 42;
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(varValue);
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(varValue);
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue(57);
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "integer",
                String.valueOf(varValue),
                ProcessVariableFilterType.EQUALS
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(varValue))
            );
    }

    @Test
    public void should_returnTasks_filteredByIntegerProcessVariableGreaterThan() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final String varName = "var-to-search";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(42);
        processVar1.setType("integer");
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(42);
        processVar2.setType("integer");
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue(10);
        processVar3.setType("integer");
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "integer",
                String.valueOf(40),
                ProcessVariableFilterType.GREATER_THAN
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (Integer) pv.getValue() > 40)
            );
    }

    @Test
    public void should_returnTasks_filteredByBigDecimalProcessVariableStoredAsStringExactQuery() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final String varName = "var-to-search";
        final String varValue = "1.2";
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(varValue);
        processVar1.setType("bigdecimal");
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(varValue);
        processVar2.setType("bigdecimal");
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue(new BigDecimal("3.4"));
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "bigdecimal",
                "1.2",
                ProcessVariableFilterType.EQUALS
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(varValue))
            );
    }

    @Test
    public void should_returnTasks_filteredByDate() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final LocalDate now = LocalDate.now();

        final String varName = "var-to-search";
        final String varValue = LocalDate.now().toString();
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(varValue);
        processVar1.setType("date");
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(varValue);
        processVar2.setType("date");
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue(now.plusDays(1).toString());
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "date",
                varValue,
                ProcessVariableFilterType.EQUALS
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdDate").descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(varValue))
            );
    }

    @Test
    public void should_returnTasks_filteredByDateGreaterThan() {
        final String processDefinitionKey = "test-process";
        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        ProcessInstanceEntity processInstance3 = createProcessInstance(processDefinitionKey);

        final LocalDate today = LocalDate.now();

        final String varName = "var-to-search";
        final String todayValue = today.toString();
        ProcessVariableEntity processVar1 = new ProcessVariableEntity();
        processVar1.setName(varName);
        processVar1.setValue(todayValue);
        processVar1.setProcessInstanceId(processInstance1.getId());
        processVar1.setProcessDefinitionKey(processInstance1.getProcessDefinitionKey());
        processVar1.setProcessInstance(processInstance1);
        variableRepository.save(processVar1);

        final String tomorrowValue = today.plusDays(1).toString();
        ProcessVariableEntity processVar2 = new ProcessVariableEntity();
        processVar2.setName(varName);
        processVar2.setValue(tomorrowValue);
        processVar2.setProcessInstanceId(processInstance2.getId());
        processVar2.setProcessDefinitionKey(processInstance2.getProcessDefinitionKey());
        processVar2.setProcessInstance(processInstance2);
        variableRepository.save(processVar2);

        final String twoDaysAgoValue = today.minusDays(2).toString();
        ProcessVariableEntity processVar3 = new ProcessVariableEntity();
        processVar3.setName(varName);
        processVar3.setValue(twoDaysAgoValue);
        processVar3.setProcessInstanceId(processInstance3.getId());
        processVar3.setProcessDefinitionKey(processInstance3.getProcessDefinitionKey());
        processVar3.setProcessInstance(processInstance3);
        variableRepository.save(processVar3);

        List<TaskEntity> tasks1 = createTasks(Set.of(processVar1), processInstance1);
        List<TaskEntity> tasks2 = createTasks(Set.of(processVar2), processInstance2);
        List<TaskEntity> tasks3 = createTasks(Set.of(processVar3), processInstance3);

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        LocalDate yesterday = today.minusDays(5);
        List<ProcessVariableValueFilter> processVariableValueFilters = List.of(
            new ProcessVariableValueFilter(
                processDefinitionKey,
                varName,
                "date",
                yesterday.toString(),
                ProcessVariableFilterType.GREATER_THAN
            )
        );

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize);

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            processVariableValueFilters,
            Collections.emptyList()
        );

        assertThat(response.getContent().size()).isEqualTo(tasks1.size() + tasks2.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && LocalDate.parse(pv.getValue()).isAfter(yesterday))
            );
    }

    @Test
    public void should_returnTasks_filteredByProcessVariable_sortedByStringVariableValue() {
        ProcessInstanceEntity processInstance = createProcessInstance("test-process");
        final String varName = "var-name";
        List<String> variableValues = Stream
            .of("beta", "alpha", "gamma", "epsilon", "delta")
            .map(value -> {
                ProcessVariableEntity processVar = new ProcessVariableEntity();
                processVar.setName(varName);
                processVar.setValue(value);
                processVar.setProcessInstanceId(processInstance.getId());
                processVar.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
                processVar.setProcessInstance(processInstance);
                variableRepository.save(processVar);
                TaskEntity task = new TaskEntity();
                task.setId(UUID.randomUUID().toString());
                task.setProcessInstance(processInstance);
                task.setProcessVariables(Set.of(processVar));
                taskRepository.save(task);
                return value;
            })
            .toList();

        Predicate predicate = null;
        VariableSearch variableSearch = new VariableSearch(null, null, null);

        List<QueryDslPredicateFilter> filters = List.of(new RootTasksFilter(false), new StandAloneTaskFilter(false));

        int pageSize = 10000;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("variables." + varName).descending());

        PagedModel<EntityModel<QueryCloudTask>> response = taskControllerHelper.findAllWithProcessVariables(
            predicate,
            variableSearch,
            pageable,
            filters,
            Collections.emptyList(),
            List.of(new ProcessVariableKey(processInstance.getProcessDefinitionKey(), varName))
        );

        assertThat(response.getContent().size()).isEqualTo(variableValues.size());
        List<QueryCloudTask> retrievedTasks = response.getContent().stream().map(EntityModel::getContent).toList();
        assertThat(retrievedTasks)
            .satisfiesExactly(
                task -> assertThat(task.getProcessVariables()).extracting("value").containsExactly("gamma"),
                task -> assertThat(task.getProcessVariables()).extracting("value").containsExactly("epsilon"),
                task -> assertThat(task.getProcessVariables()).extracting("value").containsExactly("delta"),
                task -> assertThat(task.getProcessVariables()).extracting("value").containsExactly("beta"),
                task -> assertThat(task.getProcessVariables()).extracting("value").containsExactly("alpha")
            );
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance(String processDefinitionKey) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey(processDefinitionKey);
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }

    @NotNull
    private List<TaskEntity> createTasks(
        Set<ProcessVariableEntity> variables,
        ProcessInstanceEntity processInstanceEntity
    ) {
        List<TaskEntity> taskEntities = new ArrayList<>();

        LocalDateTime start = LocalDateTime.fromDateFields(new Date());
        for (int i = 0; i < 10; i++) {
            TaskEntity taskEntity = new TaskEntity();
            String taskId = UUID.randomUUID().toString();
            taskEntity.setId(taskId);
            taskEntity.setCreatedDate(start.plusSeconds(i).toDate());
            TaskCandidateGroupEntity groupCand = new TaskCandidateGroupEntity(taskId, "group" + i);
            taskEntity.setTaskCandidateGroups(Set.of(groupCand));
            TaskCandidateUserEntity usrCand = new TaskCandidateUserEntity(taskId, "user" + i);
            taskEntity.setTaskCandidateUsers(Set.of(usrCand));
            taskEntity.setProcessVariables(variables);
            taskEntity.setProcessInstance(processInstanceEntity);
            taskEntity.setProcessInstanceId(processInstanceEntity.getId());
            taskEntities.add(taskEntity);
            taskCandidateGroupRepository.save(groupCand);
            taskCandidateUserRepository.save(usrCand);
            taskRepository.save(taskEntity);
        }
        return taskEntities;
    }
}
