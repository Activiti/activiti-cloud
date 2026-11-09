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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.conf.QueryRestWebMvcAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceHierarchyRepository;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ApplicationAdminControllerV2.class)
@Import({
    QueryRestWebMvcAutoConfiguration.class,
    CommonModelAutoConfiguration.class,
    AlfrescoWebAutoConfiguration.class,
})
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@WithMockUser
public class ApplicationAdminControllerV2IT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationRepository applicationRepository;

    @MockitoBean
    private ProcessInstanceRepository processInstanceRepository;

    @MockitoBean
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @MockitoBean
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @MockitoBean
    private SecurityManager securityManager;

    @MockitoBean
    private SecurityPoliciesManager securityPoliciesManager;

    @MockitoBean
    private SecurityPoliciesProperties securityPoliciesProperties;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private VariableRepository processVariableRepository;

    @MockitoBean
    private ProcessInstanceAdminService processInstanceAdminService;

    @MockitoBean
    private ProcessInstanceService processInstanceService;

    @MockitoBean
    private ProcessInstanceHierarchyRepository processInstanceHierarchyRepository;

    @MockitoBean
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        assertThat(processInstanceAdminService).isNotNull();
        assertThat(processInstanceService).isNotNull();
        assertThat(entityManagerFactory).isNotNull();
    }

    @Test
    public void shouldSortByVersionDescendingByDefault() throws Exception {
        List<ApplicationEntity> all = buildApplications(3);
        ArgumentCaptor<Pageable> captor = givenRepositoryContaining(all);

        mockMvc.perform(get("/admin/v2/applications").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        assertThat(captor.getValue().getSort()).isInstanceOf(QSort.class).hasSize(3);
    }

    @Test
    public void shouldReturnPageSizeOfTenByDefault() throws Exception {
        ArgumentCaptor<Pageable> captor = givenRepositoryContaining(buildApplications(25));

        String json = mockMvc
            .perform(get("/admin/v2/applications").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThatJson(json).inPath("list.pagination.count").isEqualTo(10);
        assertThatJson(json).inPath("list.pagination.hasMoreItems").isEqualTo(true);
    }

    @Test
    public void shouldKeepRequestedSortWhenProvided() throws Exception {
        ArgumentCaptor<Pageable> captor = givenRepositoryContaining(buildApplications(3));

        mockMvc
            .perform(get("/admin/v2/applications?sort=name,asc").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        assertThat(captor.getValue().getSort()).containsExactly(Sort.Order.asc("name"));
    }

    @Test
    public void shouldReturnHasMoreItemsFalseWhenLastPage() throws Exception {
        givenRepositoryContaining(buildApplications(5));

        MvcResult result = mockMvc
            .perform(get("/admin/v2/applications?skipCount=0&maxItems=10").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThatJson(json).inPath("list.pagination.count").isEqualTo(5);
        assertThatJson(json).inPath("list.pagination.hasMoreItems").isEqualTo(false);
    }

    @Test
    public void shouldReturnHasMoreItemsTrueWhenMoreItemsExist() throws Exception {
        givenRepositoryContaining(buildApplications(15));

        MvcResult result = mockMvc
            .perform(get("/admin/v2/applications?skipCount=0&maxItems=10").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThatJson(json).inPath("list.pagination.count").isEqualTo(10);
        assertThatJson(json).inPath("list.pagination.hasMoreItems").isEqualTo(true);
    }

    @Test
    public void shouldPaginateMoreThanOneThousandItemsInDescendingOrder() throws Exception {
        int total = 2500;
        givenRepositoryContaining(buildApplications(total));

        // first page: 1000 items (the max page size), more items remain
        String first = getPage(0, 1000);
        assertThatJson(first).inPath("list.pagination.count").isEqualTo(1000);
        assertThatJson(first).inPath("list.pagination.hasMoreItems").isEqualTo(true);
        assertThatJson(first).inPath("list.entries[0].entry.name").isEqualTo("app-2499");
        assertThatJson(first).inPath("list.entries[999].entry.name").isEqualTo("app-1500");

        // second page starts beyond the 1000th item and still has more items
        String second = getPage(1000, 1000);
        assertThatJson(second).inPath("list.pagination.skipCount").isEqualTo(1000);
        assertThatJson(second).inPath("list.pagination.count").isEqualTo(1000);
        assertThatJson(second).inPath("list.pagination.hasMoreItems").isEqualTo(true);
        assertThatJson(second).inPath("list.entries[0].entry.name").isEqualTo("app-1499");

        // last page: remaining 500 items, no more items
        String last = getPage(2000, 1000);
        assertThatJson(last).inPath("list.pagination.skipCount").isEqualTo(2000);
        assertThatJson(last).inPath("list.pagination.count").isEqualTo(500);
        assertThatJson(last).inPath("list.pagination.hasMoreItems").isEqualTo(false);
        assertThatJson(last).inPath("list.entries[499].entry.name").isEqualTo("app-0");
    }

    @Test
    public void shouldReturnHasMoreItemsFalseWhenPageEndsExactlyAtLastItem() throws Exception {
        givenRepositoryContaining(buildApplications(1500));

        String json = getPage(1000, 500);

        assertThatJson(json).inPath("list.pagination.count").isEqualTo(500);
        assertThatJson(json).inPath("list.pagination.hasMoreItems").isEqualTo(false);
    }

    @Test
    public void shouldRejectMaxItemsGreaterThanLimit() throws Exception {
        givenRepositoryContaining(buildApplications(1500));

        mockMvc
            .perform(get("/admin/v2/applications?skipCount=0&maxItems=1001").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError());
    }

    private String getPage(int skipCount, int maxItems) throws Exception {
        return mockMvc
            .perform(
                get("/admin/v2/applications?skipCount={s}&maxItems={m}", skipCount, maxItems).accept(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    /**
     * Mimics the repository: items are held in descending default order (the sort applied by the service),
     * and the requested slice is cut out of them using the pageable offset and size.
     */
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Pageable> givenRepositoryContaining(List<ApplicationEntity> ascending) {
        List<ApplicationEntity> descending = new ArrayList<>(ascending);
        Collections.reverse(descending);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        FluentQuery.FetchableFluentQuery<ApplicationEntity> fluentQuery = mock(FluentQuery.FetchableFluentQuery.class);
        given(fluentQuery.slice(captor.capture())).willAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            int from = (int) Math.min(pageable.getOffset(), descending.size());
            int to = Math.min(from + pageable.getPageSize(), descending.size());
            return new SliceImpl<>(descending.subList(from, to), pageable, to < descending.size());
        });
        given(applicationRepository.findBy(any(Predicate.class), any())).willAnswer(invocation -> {
            Function<FluentQuery.FetchableFluentQuery<ApplicationEntity>, Object> function = invocation.getArgument(1);
            return function.apply(fluentQuery);
        });
        return captor;
    }

    private List<ApplicationEntity> buildApplications(int count) {
        List<ApplicationEntity> applications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            applications.add(new ApplicationEntity(String.format("%06d", i), "app-" + i, "1"));
        }
        return applications;
    }
}
