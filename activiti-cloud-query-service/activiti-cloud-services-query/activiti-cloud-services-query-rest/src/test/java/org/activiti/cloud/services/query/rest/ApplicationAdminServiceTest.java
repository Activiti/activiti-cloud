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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.function.Function;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

class ApplicationAdminServiceTest {

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final ApplicationAdminService service = new ApplicationAdminService(repository);

    @Test
    void shouldSortByVersionDescendingWhenUnsorted() {
        ArgumentCaptor<Pageable> captor = givenSlice(false, 1);

        service.findAllSliced(null, PageRequest.of(0, 10));

        assertThat(captor.getValue().getSort()).containsExactly(Sort.Order.desc("version"), Sort.Order.desc("id"));
    }

    @Test
    void shouldKeepRequestedSort() {
        ArgumentCaptor<Pageable> captor = givenSlice(false, 1);

        service.findAllSliced(null, PageRequest.of(0, 10, Sort.by("name")));

        assertThat(captor.getValue().getSort()).containsExactly(Sort.Order.asc("name"));
    }

    @Test
    void shouldKeepSkipCountAndApplyDefaultSortOnAlfrescoPageRequest() {
        ArgumentCaptor<Pageable> captor = givenSlice(false, 1);

        service.findAllSliced(null, new AlfrescoPageRequest(1000, 10, PageRequest.of(0, 10)));

        Pageable pageable = captor.getValue();
        assertThat(pageable).isInstanceOf(AlfrescoPageRequest.class);
        assertThat(pageable.getOffset()).isEqualTo(1000);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("version"), Sort.Order.desc("id"));
    }

    @Test
    void shouldReportOneExtraKnownElementWhenSliceHasNext() {
        givenSlice(true, 10);

        Page<ApplicationEntity> page = service.findAllSliced(null, PageRequest.of(2, 10));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(31);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void shouldNotReportMoreElementsWhenSliceHasNoNext() {
        givenSlice(false, 4);

        Page<ApplicationEntity> page = service.findAllSliced(null, PageRequest.of(2, 10));

        assertThat(page.getTotalElements()).isEqualTo(24);
        assertThat(page.hasNext()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Pageable> givenSlice(boolean hasNext, int size) {
        List<ApplicationEntity> content = java.util.stream.IntStream.range(0, size)
            .mapToObj(i -> new ApplicationEntity("id" + i, "name" + i, "1"))
            .toList();
        FluentQuery.FetchableFluentQuery<ApplicationEntity> fluentQuery = mock(FluentQuery.FetchableFluentQuery.class);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        given(fluentQuery.slice(captor.capture())).willAnswer(invocation ->
            new SliceImpl<>(content, invocation.getArgument(0), hasNext)
        );
        given(repository.findBy(any(Predicate.class), any())).willAnswer(invocation -> {
            Function<FluentQuery.FetchableFluentQuery<ApplicationEntity>, Object> function = invocation.getArgument(1);
            return function.apply(fluentQuery);
        });
        return captor;
    }
}
