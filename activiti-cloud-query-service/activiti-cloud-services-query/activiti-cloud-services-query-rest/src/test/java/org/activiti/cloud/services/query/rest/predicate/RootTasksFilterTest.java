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

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.Predicate;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.junit.jupiter.api.Test;

public class RootTasksFilterTest {

    @Test
    public void should_addPredicateParentTaskIdIsNull_when_isRootTaskOnly() {
        //given
        RootTasksFilter rootTasksFilter = new RootTasksFilter(true);
        Predicate initialPredicate = QTaskEntity.taskEntity.name.eq("Task1");

        //when
        Predicate extendedPredicate = rootTasksFilter.extend(initialPredicate);

        //then
        assertThat(extendedPredicate).isEqualTo(QTaskEntity.taskEntity.parentTaskId.isNull().and(initialPredicate));
    }

    @Test
    public void should_returnInitialPredicate_when_isNotRootTaskOnly() {
        //given
        RootTasksFilter rootTasksFilter = new RootTasksFilter(false);
        Predicate initialPredicate = QTaskEntity.taskEntity.name.eq("Task1");

        //when
        Predicate extendedPredicate = rootTasksFilter.extend(initialPredicate);

        //then
        assertThat(extendedPredicate).isEqualTo(initialPredicate);
    }
}
