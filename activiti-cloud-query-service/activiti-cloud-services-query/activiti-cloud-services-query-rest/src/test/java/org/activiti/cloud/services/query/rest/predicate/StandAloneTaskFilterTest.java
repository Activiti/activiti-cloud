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

public class StandAloneTaskFilterTest {

    @Test
    public void should_addProcessInstanceIsNullPredicate_when_isStandAlone() {
        //given
        StandAloneTaskFilter standAloneTaskFilter = new StandAloneTaskFilter(true);
        Predicate currentPredicate = QTaskEntity.taskEntity.name.eq("Task1");

        //when
        Predicate extendedPredicate = standAloneTaskFilter.extend(currentPredicate);

        //then
        assertThat(extendedPredicate)
            .isEqualTo(QTaskEntity.taskEntity.processInstanceId.isNull().and(currentPredicate));
    }

    @Test
    public void should_returnInitialPredicate_when_isNotStandAlone() {
        //given
        StandAloneTaskFilter standAloneTaskFilter = new StandAloneTaskFilter(false);
        Predicate currentPredicate = QTaskEntity.taskEntity.name.eq("Task1");

        //when
        Predicate extendedPredicate = standAloneTaskFilter.extend(currentPredicate);

        //then
        assertThat(extendedPredicate).isEqualTo(currentPredicate);
    }
}
