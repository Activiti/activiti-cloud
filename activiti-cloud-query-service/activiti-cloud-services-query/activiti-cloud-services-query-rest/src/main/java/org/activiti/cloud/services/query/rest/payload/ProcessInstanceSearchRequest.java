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
package org.activiti.cloud.services.query.rest.payload;

import java.util.Date;
import java.util.Set;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;

public record ProcessInstanceSearchRequest(
    Set<String> name, Set<String> initiator, Set<String> appVersion, Date lastModifiedFrom, Date lastModifiedTo, Date startFrom, Date startTo, Date completedFrom, Date completedTo, Date suspendedFrom, Date suspendedTo, Set<VariableFilter> processVariableFilters, Set<ProcessVariableKey> processVariableKeys
) {}
