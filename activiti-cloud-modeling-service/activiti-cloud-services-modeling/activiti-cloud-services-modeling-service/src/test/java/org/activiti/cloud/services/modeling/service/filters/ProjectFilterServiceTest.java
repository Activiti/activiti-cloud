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

package org.activiti.cloud.services.modeling.service.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectFilterServiceTest {

    private static final String EXAMPLE_NAME = "example";
    private static final String WRONG_NAME = "wrongfilter";
    private static final List<String> ONE_TWO_PROJECT_IDS = List.of("1", "2");
    private static final List<String> ONE_PROJECT_ID = List.of("1");

    @Test
    void should_getFilteredIds() {
        final ProjectFilter projectFilter = getProjectFilter(EXAMPLE_NAME, ONE_TWO_PROJECT_IDS);
        final ProjectFilterService projectFilterService = new ProjectFilterService(List.of(projectFilter));

        List<String> filteredProjectIds = projectFilterService.getFilterIds(List.of(EXAMPLE_NAME));

        assertThat(filteredProjectIds).isEqualTo(ONE_TWO_PROJECT_IDS);
    }

    @Test
    void should_getEmptyList_when_noFiltersMatch() {
        final ProjectFilter projectFilter = getProjectFilter(EXAMPLE_NAME, ONE_TWO_PROJECT_IDS);
        final ProjectFilterService projectFilterService = new ProjectFilterService(List.of(projectFilter));

        List<String> filteredProjectIds = projectFilterService.getFilterIds(List.of(WRONG_NAME));

        assertThat(filteredProjectIds.isEmpty()).isTrue();
    }

    @Test
    void should_getEmptyList_when_someFiltersDontMatch() {
        final ProjectFilter projectFilter = getProjectFilter(EXAMPLE_NAME, ONE_TWO_PROJECT_IDS);
        final ProjectFilterService projectFilterService = new ProjectFilterService(List.of(projectFilter));

        List<String> filteredProjectIds = projectFilterService.getFilterIds(List.of(EXAMPLE_NAME, WRONG_NAME));

        assertThat(filteredProjectIds.isEmpty()).isTrue();
    }

    @Test
    void should_getIntersectionOfResults_when_multipleFilters() {
        final ProjectFilter projectFilterOne = getProjectFilter("one", ONE_PROJECT_ID);
        final ProjectFilter projectFilterOneTwo = getProjectFilter("onetwo", ONE_TWO_PROJECT_IDS);
        final ProjectFilterService projectFilterService = new ProjectFilterService(
            List.of(projectFilterOne, projectFilterOneTwo)
        );

        List<String> filteredProjectIds = projectFilterService.getFilterIds(List.of("one", "onetwo"));

        assertThat(filteredProjectIds).isEqualTo(ONE_PROJECT_ID);
    }

    private ProjectFilter getProjectFilter(String filterName, List<String> result) {
        return new ProjectFilter() {
            @Override
            public String filterName() {
                return filterName;
            }

            @Override
            public List<String> getFilterIds() {
                return result;
            }
        };
    }
}
