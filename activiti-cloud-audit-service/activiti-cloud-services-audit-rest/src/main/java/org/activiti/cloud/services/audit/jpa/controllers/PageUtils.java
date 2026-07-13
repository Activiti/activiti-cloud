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
package org.activiti.cloud.services.audit.jpa.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

class PageUtils {

    private PageUtils() {}

    /**
     * Derives a {@code totalElements} value from a {@link Slice} without executing a COUNT query.
     *
     * <p>On the last page the value is exact ({@code offset + numberOfElements}).
     * On non-last pages a minimum estimate of {@code offset + pageSize + 1} is returned to signal
     * that at least one more element exists beyond the current page. This intentionally avoids the
     * expensive {@code SELECT COUNT(*)} query that becomes a bottleneck on large tables.
     */
    static long totalElements(Slice<?> slice, Pageable pageable) {
        if (!slice.hasNext()) {
            return pageable.getOffset() + slice.getNumberOfElements();
        }
        return pageable.getOffset() + pageable.getPageSize() + 1;
    }
}
