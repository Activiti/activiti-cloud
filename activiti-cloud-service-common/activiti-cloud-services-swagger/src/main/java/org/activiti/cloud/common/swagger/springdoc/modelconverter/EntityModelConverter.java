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
import org.activiti.cloud.alfresco.rest.model.EntryResponseContent;
import org.springframework.hateoas.EntityModel;

public class EntityModelConverter extends InternalModelConverter<EntryResponseContent> {

    @Override
    protected boolean applies(JavaType javaType) {
        return EntityModel.class.equals(javaType.getRawClass());
    }

    @Override
    protected Class<EntryResponseContent> getAlternateTypeClass() {
        return EntryResponseContent.class;
    }

    @Override
    protected JavaType getContainedType(JavaType javaType) {
        return javaType.containedType(0);
    }
}
