package org.activiti.cloud.services.query.rest.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

public class DistinctWithSubQuerySpecification<T> implements Specification<T> {

    private final Specification<T> specification;
    private final String idAttribute;

    public DistinctWithSubQuerySpecification(Specification<T> specification, String idAttribute) {
        this.specification = specification;
        this.idAttribute = idAttribute;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        CriteriaQuery<Object> query1 = criteriaBuilder.createQuery();
        Subquery<?> subquery = query1.subquery(root.get(idAttribute).getModel().getBindableJavaType());
        Root<T> from = subquery.from(root.getModel());
        subquery.select(from.get(idAttribute)).where(specification.toPredicate(from, query1, criteriaBuilder));
        return root.get(idAttribute).in(subquery);
    }
}
