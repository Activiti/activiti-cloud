package org.activiti.cloud.services.query.app.repository;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.Collections;
import org.activiti.cloud.services.query.app.repository.annotation.CountOverFullWindow;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;

public class CustomizedJpaSpecificationExecutorImpl<T, ID extends Serializable>
    extends SimpleJpaRepository<T, ID>
    implements CustomizedJpaSpecificationExecutor<T> {

    private final EntityManager entityManager;

    public CustomizedJpaSpecificationExecutorImpl(
        JpaEntityInformation<T, ?> entityInformation,
        EntityManager entityManager
    ) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    protected <S extends T> TypedQuery<Long> getCountQuery(@Nullable Specification<S> spec, Class<S> domainClass) {
        if (spec != null && spec.getClass().isAnnotationPresent(CountOverFullWindow.class)) {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> query = builder.createQuery(Long.class);
            Assert.notNull(domainClass, "Domain class must not be null");
            Predicate predicate = spec.toPredicate(query.from(domainClass), query, builder);
            if (predicate != null) {
                query.where(predicate);
            }
            query.select(builder.function("COUNT_OVER", Long.class));
            query.orderBy(Collections.emptyList());
            TypedQuery<Long> typedQuery = entityManager.createQuery(query);
            typedQuery.setMaxResults(1);
            return typedQuery;
        } else {
            return super.getCountQuery(spec, domainClass);
        }
    }
}
