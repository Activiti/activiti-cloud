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

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import java.util.List;
import org.activiti.cloud.services.query.model.QServiceTaskEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.querydsl.QSort;

final class ServiceTaskSorting {

    private static final String SERVICE_TASK_TYPE = "serviceTaskType";

    private static final PathBuilder<ServiceTaskEntity> ROOT = new PathBuilder<>(
        ServiceTaskEntity.class,
        QServiceTaskEntity.serviceTaskEntity.getMetadata()
    );

    private ServiceTaskSorting() {}

    static Pageable resolve(Pageable pageable) {
        if (pageable.getSort().getOrderFor(SERVICE_TASK_TYPE) == null) {
            return pageable;
        }
        List<OrderSpecifier<?>> orderSpecifiers = pageable
            .getSort()
            .stream()
            .map(ServiceTaskSorting::toOrderSpecifier)
            .toList();
        return new SortOverride(pageable, new QSort(orderSpecifiers));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static OrderSpecifier<?> toOrderSpecifier(Sort.Order order) {
        Order direction = order.isAscending() ? Order.ASC : Order.DESC;
        Expression expression = SERVICE_TASK_TYPE.equals(order.getProperty())
            ? QServiceTaskEntity.serviceTaskEntity.serviceTaskType.coalesce(ServiceTaskEntity.DEFAULT_SERVICE_TASK_TYPE)
            : buildPropertyPathFrom(order);
        return new OrderSpecifier(direction, expression, toNullHandling(order.getNullHandling()));
    }

    private static Expression<?> buildPropertyPathFrom(Sort.Order order) {
        QueryUtils.checkSortExpression(order);

        PropertyPath path = PropertyPath.from(order.getProperty(), ROOT.getType());
        Expression<?> expression = ROOT;
        while (path != null) {
            if (!path.hasNext() && order.isIgnoreCase() && String.class.equals(path.getType())) {
                expression = Expressions.stringPath((Path<?>) expression, path.getSegment()).lower();
            } else {
                expression = Expressions.path(path.getType(), (Path<?>) expression, path.getSegment());
            }
            path = path.next();
        }
        return expression;
    }

    private static OrderSpecifier.NullHandling toNullHandling(Sort.NullHandling nullHandling) {
        return switch (nullHandling) {
            case NULLS_FIRST -> OrderSpecifier.NullHandling.NullsFirst;
            case NULLS_LAST -> OrderSpecifier.NullHandling.NullsLast;
            default -> OrderSpecifier.NullHandling.Default;
        };
    }

    /**
     * Swaps in the rewritten sort while leaving the rest of the incoming {@link Pageable} untouched. Rebuilding it as a
     * plain page request instead would lose the offset of an
     * {@code org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest}, whose {@code skipCount} is not
     * necessarily a multiple of the page size.
     */
    private record SortOverride(Pageable delegate, QSort sort) implements Pageable {
        @Override
        public boolean isPaged() {
            return delegate.isPaged();
        }

        @Override
        public int getPageNumber() {
            return delegate.getPageNumber();
        }

        @Override
        public int getPageSize() {
            return delegate.getPageSize();
        }

        @Override
        public long getOffset() {
            return delegate.getOffset();
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new SortOverride(delegate.next(), sort);
        }

        @Override
        public Pageable previousOrFirst() {
            return new SortOverride(delegate.previousOrFirst(), sort);
        }

        @Override
        public Pageable first() {
            return new SortOverride(delegate.first(), sort);
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return new SortOverride(delegate.withPage(pageNumber), sort);
        }

        @Override
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }
    }
}
