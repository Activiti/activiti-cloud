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
package org.activiti.cloud.acc.core.rest.encoders;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableQueryEncoder implements Encoder {

    private final Encoder delegate;

    public PageableQueryEncoder(Encoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        if (object instanceof Pageable) {
            Pageable pageable = (Pageable) object;
            template.query("page", String.valueOf(pageable.getPageNumber()));
            template.query("size", String.valueOf(pageable.getPageSize()));

            if (pageable.getSort() != null) {
                Collection<String> existingSorts = template.queries().get("sort");
                List<String> sortQueries = existingSorts != null ? new ArrayList<>(existingSorts) : new ArrayList<>();
                for (Sort.Order order : pageable.getSort()) {
                    sortQueries.add(order.getProperty() + "," + order.getDirection());
                }
                template.query("sort", sortQueries);
            }
        } else {
            delegate.encode(object, bodyType, template);
        }
    }
}
