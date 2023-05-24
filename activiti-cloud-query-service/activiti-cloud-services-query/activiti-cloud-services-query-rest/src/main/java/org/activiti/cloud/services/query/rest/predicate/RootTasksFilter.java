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

package org.activiti.cloud.services.query.rest.predicate;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.validation.constraints.NotNull;
import org.activiti.cloud.services.query.model.QTaskEntity;

public class RootTasksFilter implements QueryDslPredicateFilter {

    private boolean rootTasksOnly;

    public RootTasksFilter(boolean rootTasksOnly) {
        this.rootTasksOnly = rootTasksOnly;
    }

    @Override
    public Predicate extend(@NotNull Predicate currentPredicate) {
        Predicate extendedPredicate = currentPredicate;
        if (rootTasksOnly) {
            BooleanExpression parentTaskNull = QTaskEntity.taskEntity.parentTaskId.isNull();
            extendedPredicate = parentTaskNull.and(currentPredicate);
        }
        return extendedPredicate;
    }
}
