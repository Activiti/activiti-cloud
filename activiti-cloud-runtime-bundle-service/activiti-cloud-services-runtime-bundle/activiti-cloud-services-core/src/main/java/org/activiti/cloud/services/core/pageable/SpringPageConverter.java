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
package org.activiti.cloud.services.core.pageable;

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.runtime.shared.query.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SpringPageConverter {

    private static String DEFAULT_ORDER_ATTRIBUTE = "id";

    public <BASIC_TYPE, FLUENT_TYPE extends BASIC_TYPE> Page<BASIC_TYPE> toSpringPage(
        Pageable pageable,
        org.activiti.api.runtime.shared.query.Page<FLUENT_TYPE> apiPage
    ) {
        List<BASIC_TYPE> list = new ArrayList<>(apiPage.getContent());
        return new PageImpl<>(list, pageable, apiPage.getTotalItems());
    }

    public org.activiti.api.runtime.shared.query.Pageable toAPIPageable(
        org.springframework.data.domain.Pageable springPageable
    ) {
        Sort.Order order = springPageable.getSort().stream().findFirst().orElse(Sort.Order.by(DEFAULT_ORDER_ATTRIBUTE));

        return org.activiti.api.runtime.shared.query.Pageable.of(
            Math.toIntExact(springPageable.getOffset()),
            springPageable.getPageSize(),
            Order.by(order.getProperty(), Order.Direction.valueOf(order.getDirection().name()))
        );
    }
}
