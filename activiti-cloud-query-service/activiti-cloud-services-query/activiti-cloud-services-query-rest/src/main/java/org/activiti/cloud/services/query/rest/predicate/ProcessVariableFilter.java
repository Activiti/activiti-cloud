package org.activiti.cloud.services.query.rest.predicate;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.util.List;
import org.activiti.cloud.services.query.model.QTaskEntity;

public class ProcessVariableFilter implements QueryDslPredicateFilter {

    private final List<String> processVariableKeys;

    public ProcessVariableFilter(List<String> processVariableKeys) {
        this.processVariableKeys = processVariableKeys;
    }

    @Override
    public Predicate extend(Predicate currentPredicate) {
        QTaskEntity taskEntity = QTaskEntity.taskEntity;
        BooleanExpression filter = taskEntity.processVariables.any().name.in(processVariableKeys);
        return currentPredicate != null ? filter.and(currentPredicate) : filter;
    }
}
