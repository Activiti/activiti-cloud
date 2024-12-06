package org.activiti.cloud.services.query.rest.payload;

import java.util.Set;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public interface ProcessVariableFilterRequest {
    Set<VariableFilter> processVariableFilters();

    CloudRuntimeEntitySort sort();
}
