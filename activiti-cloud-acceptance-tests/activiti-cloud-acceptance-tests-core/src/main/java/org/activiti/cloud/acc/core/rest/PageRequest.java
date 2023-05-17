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

package org.activiti.cloud.acc.core.rest;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Clone Java Bean implementation of {@link Pageable} to fix https://github.com/spring-cloud/spring-cloud-openfeign/issues/854
 */
public class PageRequest extends AbstractPageRequest {

    private final Sort sort;

    /**
     * Creates a new {@link PageRequest} with sort parameters applied.
     *
     * @param page zero-based page index, must not be negative.
     * @param size the size of the page to be returned, must be greater than 0.
     * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
     */
    protected PageRequest(int page, int size, Sort sort) {

        super(page, size);

        Assert.notNull(sort, "Sort must not be null");

        this.sort = sort;
    }

    /**
     * Creates a new unsorted {@link PageRequest}.
     *
     * @param page zero-based page index, must not be negative.
     * @param size the size of the page to be returned, must be greater than 0.
     * @since 2.0
     */
    public static PageRequest of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }

    /**
     * Creates a new {@link PageRequest} with sort parameters applied.
     *
     * @param page zero-based page index.
     * @param size the size of the page to be returned.
     * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
     * @since 2.0
     */
    public static PageRequest of(int page, int size, Sort sort) {
        return new PageRequest(page, size, sort);
    }

    /**
     * Creates a new {@link PageRequest} with sort direction and properties applied.
     *
     * @param page zero-based page index, must not be negative.
     * @param size the size of the page to be returned, must be greater than 0.
     * @param direction must not be {@literal null}.
     * @param properties must not be {@literal null}.
     * @since 2.0
     */
    public static PageRequest of(int page, int size, Sort.Direction direction, String... properties) {
        return of(page, size, Sort.by(direction, properties));
    }

    /**
     * Creates a new {@link PageRequest} for the first page (page number {@code 0}) given {@code pageSize} .
     *
     * @param pageSize the size of the page to be returned, must be greater than 0.
     * @return a new {@link PageRequest}.
     * @since 2.5
     */
    public static PageRequest ofSize(int pageSize) {
        return PageRequest.of(0, pageSize);
    }

    public Sort getSort() {
        return sort;
    }

    @Override
    public PageRequest next() {
        return new PageRequest(getPageNumber() + 1, getPageSize(), getSort());
    }

    @Override
    public PageRequest previous() {
        return getPageNumber() == 0 ? this : new PageRequest(getPageNumber() - 1, getPageSize(), getSort());
    }

    @Override
    public PageRequest first() {
        return new PageRequest(0, getPageSize(), getSort());
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PageRequest that)) {
            return false;
        }

        return super.equals(that) && this.sort.equals(that.sort);
    }

    /**
     * Creates a new {@link PageRequest} with {@code pageNumber} applied.
     *
     * @param pageNumber
     * @return a new {@link PageRequest}.
     * @since 2.5
     */
    @Override
    public PageRequest withPage(int pageNumber) {
        return new PageRequest(pageNumber, getPageSize(), getSort());
    }

    /**
     * Creates a new {@link PageRequest} with {@link Sort.Direction} and {@code properties} applied.
     *
     * @param direction must not be {@literal null}.
     * @param properties must not be {@literal null}.
     * @return a new {@link PageRequest}.
     * @since 2.5
     */
    public PageRequest withSort(Sort.Direction direction, String... properties) {
        return new PageRequest(getPageNumber(), getPageSize(), Sort.by(direction, properties));
    }

    /**
     * Creates a new {@link PageRequest} with {@link Sort} applied.
     *
     * @param sort must not be {@literal null}.
     * @return a new {@link PageRequest}.
     * @since 2.5
     */
    public PageRequest withSort(Sort sort) {
        return new PageRequest(getPageNumber(), getPageSize(), sort);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + sort.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Page request [number: %d, size %d, sort: %s]", getPageNumber(), getPageSize(), sort);
    }

}
