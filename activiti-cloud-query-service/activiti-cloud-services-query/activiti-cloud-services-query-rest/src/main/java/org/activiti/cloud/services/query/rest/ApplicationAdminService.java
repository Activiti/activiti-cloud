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

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageRequest;
import org.activiti.cloud.services.query.app.repository.ApplicationRepository;
import org.activiti.cloud.services.query.model.ApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

public class ApplicationAdminService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("version"), Sort.Order.desc("id"));

    private final ApplicationRepository repository;

    public ApplicationAdminService(ApplicationRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds applications using slice-based pagination, sorted by {@code version} descending (then {@code id} descending, as a tie-breaker
     * for stable paging) unless a sort is requested.
     * <p>
     * No {@code COUNT} query is executed: one extra element is fetched to know whether a next page exists, and the
     * returned page reports an estimated total derived from the current offset.
     */
    public Page<ApplicationEntity> findAllSliced(Predicate predicate, Pageable pageable) {
        Predicate resolvedPredicate = Optional.ofNullable(predicate).orElseGet(BooleanBuilder::new);
        Pageable resolvedPageable = applyDefaultSort(pageable);

        Slice<ApplicationEntity> slice = repository.findBy(resolvedPredicate, query -> query.slice(resolvedPageable));
        List<ApplicationEntity> content = slice.getContent();

        long knownElements = resolvedPageable.getOffset() + content.size() + (slice.hasNext() ? 1 : 0);

        return new PageImpl<>(content, resolvedPageable, knownElements);
    }

    private Pageable applyDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        if (pageable instanceof AlfrescoPageRequest alfrescoPageRequest) {
            Pageable inner = alfrescoPageRequest.getPageable();
            return new AlfrescoPageRequest(
                alfrescoPageRequest.getOffset(),
                alfrescoPageRequest.getPageSize(),
                PageRequest.of(inner.getPageNumber(), inner.getPageSize(), DEFAULT_SORT)
            );
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
    }
}
