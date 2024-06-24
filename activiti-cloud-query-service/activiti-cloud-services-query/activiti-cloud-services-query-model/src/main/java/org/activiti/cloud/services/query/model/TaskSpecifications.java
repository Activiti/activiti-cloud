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
package org.activiti.cloud.services.query.model;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {

    public static Specification<TaskEntity> withDynamicConditions(TaskSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            Join<TaskEntity, ProcessVariableEntity> pvJoin = root.joinSet(TaskEntity_.PROCESS_VARIABLES);

            Predicate predicate = criteriaBuilder.disjunction();

            for (ProcessVariableNameValuePair pair : criteria.conditions()) {
                predicate =
                    criteriaBuilder.or(
                        predicate,
                        criteriaBuilder.and(
                            criteriaBuilder.equal(pvJoin.get(ProcessVariableEntity_.name), pair.getName()),
                            criteriaBuilder.like(pvJoin.get("value"), "%" + pair.getValue() + "%")
                        )
                    );
            }

            query.groupBy(root.get(TaskEntity_.id));

            List<Predicate> havingPredicates = new ArrayList<>();

            for (ProcessVariableNameValuePair pair : criteria.conditions()) {
                Predicate conditionPredicate = criteriaBuilder.and(
                    criteriaBuilder.equal(pvJoin.get(ProcessVariableEntity_.name), pair.getName()),
                    criteriaBuilder.like(pvJoin.get("value"), "%" + pair.getValue() + "%")
                );

                Predicate havingPredicate = criteriaBuilder.gt(
                    criteriaBuilder.countDistinct(
                        criteriaBuilder
                            .selectCase()
                            .when(conditionPredicate, pvJoin.get(ProcessVariableEntity_.id))
                            .otherwise(criteriaBuilder.nullLiteral(Long.class))
                    ),
                    0L
                );

                havingPredicates.add(havingPredicate);
            }

            Predicate finalHavingPredicate = criteriaBuilder.and(havingPredicates.toArray(new Predicate[0]));

            query.having(finalHavingPredicate);

            return predicate;
        };
    }
}
