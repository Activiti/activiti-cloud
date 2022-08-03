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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectFilterService {

    private final Map<String, ProjectFilter> projectFilters;

    public ProjectFilterService(List<ProjectFilter> projectFilters) {
        this.projectFilters = projectFilters.stream()
            .collect(Collectors.toMap(ProjectFilter::filterName, projectFilter -> projectFilter));
    }

    public List<String> getFilterIds(List<String> filterNames) {
        List<List<String>> allFilters = filterNames.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::toLowerCase)
            .map(projectFilters::get)
            .map(getFilterIds())
            .collect(Collectors.toList());

        switch (allFilters.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return allFilters.get(0);
            default:
                return intersection(allFilters);
        }
    }

    private Function<ProjectFilter, List<String>> getFilterIds() {
        return projectFilter -> projectFilter == null ? Collections.emptyList() : projectFilter.getFilterIds();
    }

    private List<String> intersection(List<List<String>> allFilters) {
        List<String> intersection = new ArrayList<>(allFilters.get(0));
        for (List<String> filteredProjects : allFilters.subList(1, allFilters.size())) {
            intersection.retainAll(filteredProjects);
        }
        return intersection;
    }
}
