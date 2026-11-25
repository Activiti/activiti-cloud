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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.services.query.model.QServiceTaskEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QSort;

class ServiceTaskSortingTest {

    private static final String SERVICE_TASK_TYPE = "serviceTaskType";

    @Test
    void should_queryWithRewrittenSortButReturnPageWithRequestedSort_when_sortedByServiceTaskType() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by(SERVICE_TASK_TYPE));
        AtomicReference<Pageable> usedByQuery = new AtomicReference<>();

        Page<String> page = ServiceTaskSorting.sorted(pageable, resolved -> {
            usedByQuery.set(resolved);
            return new PageImpl<>(List.of("a", "b"), resolved, 7);
        });

        //the query runs with the coalesced ordering
        assertThat(usedByQuery.get().getSort()).isInstanceOf(QSort.class);
        //the page handed to the model assembler keeps the property name the client asked for, so that the
        //pagination links it builds stay followable
        assertThat(page.getSort().getOrderFor(SERVICE_TASK_TYPE)).isNotNull();
        assertThat(page.getSort()).allSatisfy(order -> assertThat(order.getProperty()).doesNotContain("coalesce"));
        assertThat(page.getContent()).containsExactly("a", "b");
        assertThat(page.getTotalElements()).isEqualTo(7);
    }

    @Test
    void should_returnQueryPageUntouched_when_sortDoesNotIncludeServiceTaskType() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("startedDate"));
        Page<String> queried = new PageImpl<>(List.of("a"), pageable, 1);

        assertThat(ServiceTaskSorting.sorted(pageable, resolved -> queried)).isSameAs(queried);
    }

    @Test
    void should_lowerCaseCoalescedType_when_orderIgnoresCase() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc(SERVICE_TASK_TYPE).ignoreCase()));

        OrderSpecifier<?> specifier = ((QSort) ServiceTaskSorting.resolve(pageable).getSort())
            .getOrderSpecifiers()
            .get(0);

        assertThat(specifier.getTarget().toString())
            .startsWith("lower(")
            .contains(ServiceTaskEntity.DEFAULT_SERVICE_TASK_TYPE);
    }

    @Test
    void should_returnPageableUnchanged_when_sortIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThat(ServiceTaskSorting.resolve(pageable)).isSameAs(pageable);
    }

    @Test
    void should_returnPageableUnchanged_when_sortDoesNotIncludeServiceTaskType() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startedDate"));

        assertThat(ServiceTaskSorting.resolve(pageable)).isSameAs(pageable);
    }

    @Test
    void should_orderByCoalescedType_when_sortedByServiceTaskType() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, SERVICE_TASK_TYPE));

        Sort resolved = ServiceTaskSorting.resolve(pageable).getSort();

        assertThat(resolved).isInstanceOf(QSort.class);
        List<OrderSpecifier<?>> orderSpecifiers = ((QSort) resolved).getOrderSpecifiers();
        assertThat(orderSpecifiers).hasSize(1);
        assertThat(orderSpecifiers.get(0).getOrder()).isEqualTo(Order.DESC);
        assertThat(orderSpecifiers.get(0).getTarget()).isNotEqualTo(
            QServiceTaskEntity.serviceTaskEntity.serviceTaskType
        );
        assertThat(orderSpecifiers.get(0).getTarget().toString()).contains(ServiceTaskEntity.DEFAULT_SERVICE_TASK_TYPE);
    }

    @Test
    void should_keepSecondarySortOnOtherProperty_when_sortedByServiceTaskType() {
        Pageable pageable = PageRequest.of(
            0,
            10,
            Sort.by(Sort.Order.asc(SERVICE_TASK_TYPE), Sort.Order.desc("startedDate"))
        );

        List<OrderSpecifier<?>> orderSpecifiers = (
            (QSort) ServiceTaskSorting.resolve(pageable).getSort()
        ).getOrderSpecifiers();

        assertThat(orderSpecifiers).hasSize(2);
        assertThat(orderSpecifiers.get(0).getOrder()).isEqualTo(Order.ASC);
        assertThat(orderSpecifiers.get(1).getOrder()).isEqualTo(Order.DESC);
        assertThat(orderSpecifiers.get(1).getTarget().toString()).isEqualTo("serviceTaskEntity.startedDate");
    }

    @Test
    void should_buildNestedPath_when_sortedByServiceTaskTypeAndNestedProperty() {
        Pageable pageable = PageRequest.of(
            0,
            10,
            Sort.by(Sort.Order.asc(SERVICE_TASK_TYPE), Sort.Order.asc("integrationContexts.connectorType"))
        );

        List<OrderSpecifier<?>> orderSpecifiers = (
            (QSort) ServiceTaskSorting.resolve(pageable).getSort()
        ).getOrderSpecifiers();

        assertThat(orderSpecifiers.get(1).getTarget().toString()).isEqualTo(
            "serviceTaskEntity.integrationContexts.connectorType"
        );
    }

    @Test
    void should_throw_when_sortedByUnknownProperty() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(SERVICE_TASK_TYPE, "notAProperty"));

        assertThatThrownBy(() -> ServiceTaskSorting.resolve(pageable)).isInstanceOf(PropertyReferenceException.class);
    }

    @Test
    void should_preserveSkipCountOffset_when_pageableIsAlfrescoPageRequest() {
        // skipCount is not a multiple of maxItems, so the page number alone cannot describe the offset
        AlfrescoPageRequest pageable = new AlfrescoPageRequest(
            25,
            10,
            PageRequest.of(0, 10, Sort.by(SERVICE_TASK_TYPE))
        );

        Pageable resolved = ServiceTaskSorting.resolve(pageable);

        assertThat(resolved.getOffset()).isEqualTo(25);
        assertThat(resolved.getPageSize()).isEqualTo(10);
        assertThat(resolved.getPageNumber()).isEqualTo(pageable.getPageNumber());
        assertThat(resolved.getSort()).isInstanceOf(QSort.class);
    }

    @Test
    void should_keepResolvedSort_when_navigatingToAnotherPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(SERVICE_TASK_TYPE));

        Pageable next = ServiceTaskSorting.resolve(pageable).next();

        assertThat(next.getOffset()).isEqualTo(10);
        assertThat(next.getSort()).isInstanceOf(QSort.class);
    }
}
