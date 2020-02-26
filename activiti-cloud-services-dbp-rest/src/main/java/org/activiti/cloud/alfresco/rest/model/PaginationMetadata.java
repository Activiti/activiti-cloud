/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.alfresco.rest.model;

public class PaginationMetadata {

    private long skipCount;

    private long maxItems;

    private long count;

    private boolean hasMoreItems;

    private long totalItems;

    public PaginationMetadata() {
    }

    public PaginationMetadata(long skipCount,
                              long maxItems,
                              long count,
                              boolean hasMoreItems,
                              long totalItems) {
        this.skipCount = skipCount;
        this.maxItems = maxItems;
        this.count = count;
        this.hasMoreItems = hasMoreItems;
        this.totalItems = totalItems;
    }

    public long getSkipCount() {
        return skipCount;
    }

    public long getMaxItems() {
        return maxItems;
    }

    public long getCount() {
        return count;
    }

    public boolean isHasMoreItems() {
        return hasMoreItems;
    }

    public long getTotalItems() {
        return totalItems;
    }

}
