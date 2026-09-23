/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.query.app.specification;

import jakarta.persistence.criteria.Predicate;

public interface VariableValueFilterCondition {
    /**
     * @return the value comparison applied to the {@code max(case when ... end)} aggregate, to be
     *         used in the {@code HAVING} clause of a query grouped by the root entity id.
     */
    Predicate getPredicate();

    /**
     * @return the variable selection predicate combined with the value comparison applied to the
     *         plain (non aggregated) extracted value, to be used in the {@code WHERE} clause of a
     *         correlated {@code EXISTS} subquery.
     */
    Predicate getSubqueryPredicate();
}
