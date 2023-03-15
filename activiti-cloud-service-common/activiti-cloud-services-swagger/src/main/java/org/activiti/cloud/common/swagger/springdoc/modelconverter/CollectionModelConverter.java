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
package org.activiti.cloud.common.swagger.springdoc.modelconverter;

import com.fasterxml.jackson.databind.JavaType;
import org.activiti.cloud.alfresco.rest.model.ListResponseContent;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

public class CollectionModelConverter extends InternalModelConverter<ListResponseContent> {

    @Override
    protected boolean applies(JavaType javaType) {
        return (
            CollectionModel.class.equals(javaType.getRawClass()) &&
            EntityModel.class.equals(javaType.containedType(0).getRawClass())
        );
    }

    @Override
    protected Class<ListResponseContent> getAlternateTypeClass() {
        return ListResponseContent.class;
    }

    @Override
    protected JavaType getContainedType(JavaType javaType) {
        return javaType.containedType(0).containedType(0);
    }
}
