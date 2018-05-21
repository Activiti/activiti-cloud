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

package org.activiti.cloud.services.core.pageable;

import org.activiti.runtime.api.query.Order;
import org.activiti.runtime.api.query.Pageable;
import org.activiti.runtime.api.query.ProcessInstanceFilter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableConverter {

    public Pageable toAPIPageable(org.springframework.data.domain.Pageable springPageable) {
        Sort.Order order = springPageable.getSort().stream().findFirst().orElse(Sort.Order.by(ProcessInstanceFilter.ID));

        return Pageable.of(Math.toIntExact(springPageable.getOffset()),
                           springPageable.getPageSize(),
                           Order.by(order.getProperty(),
                                    Order.Direction.valueOf(order.getDirection().name())));
    }
}
