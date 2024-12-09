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
package org.activiti.cloud.services.query.app.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used at class level on a specification executed by {@link org.activiti.cloud.services.query.app.repository.CustomizedJpaSpecificationExecutorImpl}
 * to count all the results over the full window when there is a group by clause in the query,
 * overriding the default behavior which counts the cardinality of the groups.
 * From an SQL perspective, this means that 'count (*) over()' is used instead of 'count (*)'.
 *
 * @see org.springframework.data.jpa.domain
 * @see org.activiti.cloud.services.query.app.repository.CustomizedJpaSpecificationExecutorImpl
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CountOverFullWindow {
}
