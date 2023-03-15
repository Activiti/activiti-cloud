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

import static org.activiti.test.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class AlfrescoPageRequestTest {

    @Test
    void getPageNumberShouldBeTheDivisionOfSkipCountOverPageSizeWhenNoRemainder() {
        //when
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(30, 10, null);

        //then
        assertThat(alfrescoPageRequest).hasPageNumber(3);
    }

    @Test
    void getPageNumberShouldBeTheDivisionOfSkipCountOverPageSizePlusOneWhenThereIsARemainder() {
        //when
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(31, 10, null);

        //then
        assertThat(alfrescoPageRequest).hasPageNumber(4);
    }

    @Test
    void previousShouldDecrementSkipCountByPageSizeWhenResultIsZeroOrPositive() {
        //given
        AlfrescoPageRequest page1 = new AlfrescoPageRequest(10, 10, null);
        AlfrescoPageRequest page2 = new AlfrescoPageRequest(20, 10, null);

        //when
        AlfrescoPageRequest previousForPage1 = page1.previous();
        AlfrescoPageRequest previousForPage2 = page2.previous();

        //then
        assertThat(previousForPage1).hasOffset(0).hasPageSize(10);
        assertThat(previousForPage2).hasOffset(10).hasPageSize(10);
    }

    @Test
    void previousShouldReturnItselfWhenSkipCountIsZero() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(0, 10, null);

        //when
        AlfrescoPageRequest previous = alfrescoPageRequest.previous();

        //then
        assertThat(previous).hasPageSize(10).hasOffset(0);
    }

    @Test
    void previousShouldReturnSmallerPageWhenThereAreNotEnoughElementsInTheSkipCount() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(3, 10, null);

        //when
        AlfrescoPageRequest previous = alfrescoPageRequest.previous();

        //then
        //there are only 3 elements skipped, so the page size should be limited to 3 to don't include elements
        // already present in the current page
        assertThat(previous).hasOffset(0).hasPageSize(3);
    }

    @Test
    void nextShouldIncrementSkipCountByPageSize() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(4, 10, null);

        //when
        AlfrescoPageRequest next = alfrescoPageRequest.next();

        //then
        assertThat(next).hasOffset(14).hasPageSize(10);
    }

    @Test
    void firstShouldReturnPageWithZeroSkipCountAndSameSizeWhenSkipCountIsDivisible() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(400, 100, null);

        //when
        AlfrescoPageRequest first = alfrescoPageRequest.first();

        //then
        assertThat(first).hasOffset(0).hasPageSize(100);
    }

    @Test
    void firstShouldReturnPageWithZeroSkipCountAndSmallerSizeWhenSkipCountIsNotDivisible() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(450, 100, null);

        //when
        AlfrescoPageRequest first = alfrescoPageRequest.first();

        //then
        assertThat(first).hasOffset(0).hasPageSize(50);
    }

    @Test
    void hasPreviousShouldReturnTrueWhenSkipCountIsGreaterThanZero() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(1, 10, null);

        //then
        assertThat(alfrescoPageRequest).hasPrevious();
    }

    @Test
    void hasPreviousShouldReturnFalseWhenSkipCountIsZero() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(0, 10, null);

        //then
        assertThat(alfrescoPageRequest).doesNotHavePrevious();
    }

    @Test
    void getSortShouldReuseBasePageableSort() {
        //given
        Sort sort = mock(Sort.class);
        PageRequest basePage = PageRequest.of(0, 10, sort);
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(0, 10, basePage);

        //then
        assertThat(alfrescoPageRequest).hasSort(sort);
    }

    @Test
    void withPage_ReturnsAPageWithMaxItemTimesPageNumber() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(4, 10, null);

        //then
        Pageable actual = alfrescoPageRequest.withPage(4);

        assertThat(actual).hasOffset(40).hasPageSize(10);
    }

    @Test
    void withPage_WithNegativePageNumberThrowsException() {
        //given
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(4, 10, null);

        //then
        assertThrows(IllegalArgumentException.class, () -> alfrescoPageRequest.withPage(-4));
    }
}
