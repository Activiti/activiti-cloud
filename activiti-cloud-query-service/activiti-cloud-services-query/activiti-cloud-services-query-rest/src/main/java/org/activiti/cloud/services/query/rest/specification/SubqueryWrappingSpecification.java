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
