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
package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public class SubqueryWrappingSpecification<T> implements Specification<T> {

    private final SpecificationSupport<T> specification;

    public SubqueryWrappingSpecification(SpecificationSupport<T> specification) {
        this.specification = specification;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        specification.setDistinct(false);
        Subquery<T> subquery = query.subquery(root.getModel().getJavaType());
        Root<T> subroot = subquery.correlate(root);
        subquery.select(subroot);
        subquery.select(subroot).where(specification.toPredicate(subroot, query, criteriaBuilder)).distinct(true);
        return root.in(subquery);
    }
}
