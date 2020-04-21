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

package org.activiti.cloud.alfresco.data.domain;

import org.junit.jupiter.api.Test;
import org.springframework.hateoas.PagedModel;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtendedPageMetadataConverterTest {

    private ExtendedPageMetadataConverter converter = new ExtendedPageMetadataConverter();

    @Test
    public void getMetadataShouldReturnTotalPagesAsDivisionOfTotalElementsByPageSizeWhenNoRemainder() throws Exception {
        //given
        PagedModel.PageMetadata basePageMetadata = new PagedModel.PageMetadata(10,
                                                                                       1,
                                                                                       30);
        //when
        ExtendedPageMetadata extendedPageMetadata = converter.toExtendedPageMetadata(10,
                                                                                     basePageMetadata);

        //then
        assertThat(extendedPageMetadata.getTotalPages()).isEqualTo(3);
    }

    @Test
    public void getMetadataShouldReturnTotalPagesAsDivisionOfTotalElementsByPageSizePlusOneWhenThereIsRemainder() throws Exception {
        //given
        PagedModel.PageMetadata basePageMetadata = new PagedModel.PageMetadata(10,
                                                                                       1,
                                                                                       31);

        //when
        ExtendedPageMetadata extendedPageMetadata = converter.toExtendedPageMetadata(10,
                                                                                       basePageMetadata);

        //then
        assertThat(extendedPageMetadata.getTotalPages()).isEqualTo(4);
    }

    @Test
    public void getMetadataShouldIncreaseTotalPagesByOneWhenSkipCountIsNotDivisibleAndTotalElementsIsDivisible() throws Exception {
        //given
        PagedModel.PageMetadata basePageMetadata = new PagedModel.PageMetadata(10,
                                                                                       1,
                                                                                       30);
        //when
        ExtendedPageMetadata extendedPageMetadata = converter.toExtendedPageMetadata(11,
                                                                                     basePageMetadata);

        //then
        //When the skip count is not divisible by page size, the first page will be considered as a smaller page
        //in this example the remainder will be 1, so the first page will contain only one element:
        // total elements 30 : [0 .. 29]
        // page 0: [0] -> 1 element
        // page 1: [1..10] -> 10 elements
        // page 2: [11..20] -> 10 elements
        // page 3: [21..29] -> 9 elements
        assertThat(extendedPageMetadata.getTotalPages()).isEqualTo(4);
    }

    @Test
    public void getMetadataShouldNotChangeTotalPagesWhenTotalElementsRemainderIsLowerThanOrEqualsToSkipCountRemainder() throws Exception {
        //given
        PagedModel.PageMetadata basePageMetadata = new PagedModel.PageMetadata(10,
                                                                                       1,
                                                                                       31);

        //when
        ExtendedPageMetadata extendedPageMetadata = converter.toExtendedPageMetadata(11,
                                                                                       basePageMetadata);

        //then
        //When the skip count is not divisible by page size, the first page will be considered as a smaller page
        //in this example the remainder will be 1, so the first page will contain only one element:
        // total elements 31 : [0 .. 30]
        // page 0: [0]
        // page 1: [1..10]
        // page 2: [11..20]
        // page 3: [21..30]
        assertThat(extendedPageMetadata.getTotalPages()).isEqualTo(4);
    }

    @Test
    public void getMetadataShouldIncreaseTotalPagesByOneWhenTotalElementsRemainderIsGreaterThanSkipCountRemainder() throws Exception {
        //given
        PagedModel.PageMetadata basePageMetadata = new PagedModel.PageMetadata(10,
                                                                                       1,
                                                                                       32);
        //when
        ExtendedPageMetadata extendedPageMetadata = converter.toExtendedPageMetadata(11,
                                                                                       basePageMetadata);
        //then
        //When the skip count is not divisible by page size, the first page will be considered as a smaller page
        //in this example the remainder will be 1, so the first page will contain only one element:
        // total elements 32 : [0 .. 31]
        // page 0: [0]
        // page 1: [1..10]
        // page 2: [11..20]
        // page 3: [21..30]
        // page 4: [31]
        assertThat(extendedPageMetadata.getTotalPages()).isEqualTo(5);
    }

}
