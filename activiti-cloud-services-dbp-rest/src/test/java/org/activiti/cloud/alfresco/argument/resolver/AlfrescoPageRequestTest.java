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

package org.activiti.cloud.alfresco.argument.resolver;

import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.activiti.test.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AlfrescoPageRequestTest {

    @Test
    public void getPageNumberShouldBeTheDivisionOfSkipCountOverPageSizeWhenNoRemainder() throws Exception {
        //given
        int skipCount = 30;
        int maxItems = 10;


        //when
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(skipCount,
                                                                          maxItems,
                                                                          null);

        //then
        assertThat(alfrescoPageRequest).hasPageNumber(3);
    }

    @Test
    public void getPageNumberShouldBeTheDivisionOfSkipCountOverPageSizePlusOneWhenThereIsARemainder() throws Exception {
        //given
        int skipCount = 31;
        int maxItems = 10;


        //when
        AlfrescoPageRequest alfrescoPageRequest = new AlfrescoPageRequest(skipCount,
                                                                          maxItems,
                                                                          null);

        //then
        assertThat(alfrescoPageRequest).hasPageNumber(4);
    }

    @Test
    public void previousShouldDecrementSkipCountByPageSizeWhenResultIsZeroOrPositive() throws Exception {
        //given
        AlfrescoPageRequest page1 = new AlfrescoPageRequest(10,
                                                            10,
                                                            null);
        AlfrescoPageRequest page2 = new AlfrescoPageRequest(20,
                                                            10,
                                                            null);

        //when
        AlfrescoPageRequest previousForPage1 = page1.previous();
        AlfrescoPageRequest previousForPage2 = page2.previous();

        //then
        assertThat(previousForPage1)
                .hasOffset(0)
                .hasPageSize(10);
        assertThat(previousForPage2)
                .hasOffset(10)
                .hasPageSize(10);
    }

    @Test
    public void previousShouldReturnItselfWhenSkipCountIsZero() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(0,
                                                           10,
                                                           null);

        //when
        AlfrescoPageRequest previous = page.previous();

        //then
        assertThat(previous).hasPageSize(10).hasOffset(0);
    }

    @Test
    public void previousShouldReturnSmallerPageWhenThereAreNotEnoughElementsInTheSkipCount() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(3,
                                                           10,
                                                           null);

        //when
        AlfrescoPageRequest previous = page.previous();

        //then
        //there are only 3 elements skipped, so the page size should be limited to 3 to don't include elements
        // already present in the current page
        assertThat(previous)
                .hasOffset(0)
                .hasPageSize(3);
    }

    @Test
    public void nextShouldIncrementSkipCountByPageSize() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(4,
                                                           10,
                                                           null);

        //when
        AlfrescoPageRequest next = page.next();

        //then
        assertThat(next)
                .hasOffset(14)
                .hasPageSize(10);
    }

    @Test
    public void firstShouldReturnPageWithZeroSkipCountAndSameSizeWhenSkipCountIsDivisible() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(400,
                                                           100,
                                                           null);

        //when
        AlfrescoPageRequest first = page.first();

        //then
        assertThat(first)
                .hasOffset(0)
                .hasPageSize(100);
    }

    @Test
    public void firstShouldReturnPageWithZeroSkipCountAndSmallerSizeWhenSkipCountIsNotDivisible() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(450,
                                                           100,
                                                           null);

        //when
        AlfrescoPageRequest first = page.first();

        //then
        assertThat(first)
                .hasOffset(0)
                .hasPageSize(50);
    }

    @Test
    public void hasPreviousShouldReturnTrueWhenSkipCountIsGreaterThanZero() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(1,
                                                           10,
                                                           null);

        //then
        assertThat(page).hasPrevious();
    }
    @Test
    public void hasPreviousShouldReturnFalseWhenSkipCountIsZero() throws Exception {
        //given
        AlfrescoPageRequest page = new AlfrescoPageRequest(0,
                                                           10,
                                                           null);

        //then
        assertThat(page).doesNotHavePrevious();
    }

    @Test
    public void getSortShouldReuseBasePageableSort() throws Exception {
        //given
        Sort sort = mock(Sort.class);
        PageRequest basePage = PageRequest.of(0,
                                        10,
                                        sort);
        AlfrescoPageRequest pageRequest = new AlfrescoPageRequest(0,
                                                                          10,
                                                                          basePage);

        //then
        assertThat(pageRequest).hasSort(sort);
    }
}