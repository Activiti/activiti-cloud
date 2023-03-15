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
package org.activiti.cloud.alfresco.argument.resolver;

import static java.lang.String.format;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class AlfrescoPageRequest extends AbstractPageRequest implements Pageable {

    private final long skipCount;
    private final Pageable pageable;

    public AlfrescoPageRequest(long skipCount, int maxItems, Pageable pageable) {
        super(0, maxItems);
        this.skipCount = skipCount;
        this.pageable = pageable;
    }

    @Override
    public Sort getSort() {
        return pageable.getSort();
    }

    @Override
    public AlfrescoPageRequest next() {
        return new AlfrescoPageRequest(skipCount + getPageSize(), getPageSize(), getPageable());
    }

    @Override
    public boolean hasPrevious() {
        return skipCount > 0;
    }

    @Override
    public int getPageNumber() {
        if (skipCount % getPageSize() == 0) {
            return Math.toIntExact(skipCount / getPageSize());
        } else {
            return Math.toIntExact(skipCount / getPageSize()) + 1;
        }
    }

    @Override
    public AlfrescoPageRequest previous() {
        if (skipCount == 0) {
            return this;
        }

        int nextPageSize = getPageSize();
        long newSkipCount = this.skipCount - getPageSize();

        if (newSkipCount < 0) {
            newSkipCount = 0;
            nextPageSize = Math.toIntExact(skipCount);
        }

        return new AlfrescoPageRequest(newSkipCount, nextPageSize, getPageable());
    }

    @Override
    public AlfrescoPageRequest first() {
        long remainder = skipCount % getPageSize();
        int pageSize = Math.toIntExact(remainder == 0 ? getPageSize() : remainder);

        return new AlfrescoPageRequest(0, pageSize, getPageable());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                format("Invalid request of a pageNumber %d. " + "pageNumber must be a non negative number", pageNumber)
            );
        }

        return new AlfrescoPageRequest((long) pageNumber * getPageSize(), getPageSize(), getPageable());
    }

    @Override
    public long getOffset() {
        return skipCount;
    }

    public Pageable getPageable() {
        return pageable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        AlfrescoPageRequest that = (AlfrescoPageRequest) o;

        if (skipCount != that.skipCount) {
            return false;
        }
        return pageable != null ? pageable.equals(that.pageable) : that.pageable == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (skipCount ^ (skipCount >>> 32));
        result = 31 * result + (pageable != null ? pageable.hashCode() : 0);
        return result;
    }
}
