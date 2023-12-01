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

package org.activiti.cloud.services.query.rest.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.querydsl.core.types.Predicate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class QueryDslPredicateAggregatorTest {

    private QueryDslPredicateAggregator predicateAggregator = new QueryDslPredicateAggregator();

    @Test
    public void should_addPredicatesFromAllFiltersToInitialPredicate() {
        //given
        Predicate initialPredicate = mock(Predicate.class);
        QueryDslPredicateFilter firstFilter = mock(QueryDslPredicateFilter.class);

        Predicate initialPredicatePlusFirstFilter = mock(Predicate.class);
        given(firstFilter.extend(initialPredicate)).willReturn(initialPredicatePlusFirstFilter);

        QueryDslPredicateFilter secondFilter = mock(QueryDslPredicateFilter.class);
        Predicate initialPredicatePlusFirstAndSecondFilters = mock(Predicate.class);
        given(secondFilter.extend(initialPredicatePlusFirstFilter))
            .willReturn(initialPredicatePlusFirstAndSecondFilters);

        //when
        Predicate finalPredicate = predicateAggregator.applyFilters(
            initialPredicate,
            Arrays.asList(firstFilter, secondFilter)
        );

        //then
        assertThat(finalPredicate).isEqualTo(initialPredicatePlusFirstAndSecondFilters);
    }
}
