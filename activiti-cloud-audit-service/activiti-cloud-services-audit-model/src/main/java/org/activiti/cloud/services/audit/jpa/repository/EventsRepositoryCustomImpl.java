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
package org.activiti.cloud.services.audit.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Collectors;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;

public class EventsRepositoryCustomImpl implements EventsRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Slice<AuditEventEntity> findAllAsSlice(Pageable pageable) {
        return findAllAsSlice(null, pageable);
    }

    @Override
    public Slice<AuditEventEntity> findAllAsSlice(Specification<AuditEventEntity> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEventEntity> query = cb.createQuery(AuditEventEntity.class);
        Root<AuditEventEntity> root = query.from(AuditEventEntity.class);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        if (pageable.getSort().isSorted()) {
            List<Order> orders = pageable.getSort().stream()
                .map(sortOrder ->
                    sortOrder.isAscending()
                        ? cb.asc(root.get(sortOrder.getProperty()))
                        : cb.desc(root.get(sortOrder.getProperty()))
                )
                .collect(Collectors.toList());
            query.orderBy(orders);
        }

        int fetchSize = pageable.getPageSize() + 1;
        List<AuditEventEntity> results = entityManager
            .createQuery(query)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(fetchSize)
            .getResultList();

        boolean hasNext = results.size() > pageable.getPageSize();
        return new SliceImpl<>(
            hasNext ? results.subList(0, pageable.getPageSize()) : results,
            pageable,
            hasNext
        );
    }
}
