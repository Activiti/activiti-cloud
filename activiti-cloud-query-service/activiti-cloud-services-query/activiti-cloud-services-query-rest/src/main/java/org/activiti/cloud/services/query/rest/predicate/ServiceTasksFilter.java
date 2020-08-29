package org.activiti.cloud.services.query.rest.predicate;

import javax.validation.constraints.NotNull;

import org.activiti.cloud.services.query.model.QBPMNActivityEntity;

import com.querydsl.core.types.Predicate;

public class ServiceTasksFilter implements QueryDslPredicateFilter {

    public static final String SERVICE_TASK = "serviceTask";

    @Override
    public Predicate extend(@NotNull Predicate currentPredicate) {
        return QBPMNActivityEntity.bPMNActivityEntity.activityType.eq(SERVICE_TASK)
                                                                  .and(currentPredicate);
    }
}
