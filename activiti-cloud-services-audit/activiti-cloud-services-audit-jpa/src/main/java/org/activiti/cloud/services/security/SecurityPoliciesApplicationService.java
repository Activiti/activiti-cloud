package org.activiti.cloud.services.security;

import java.util.Map;
import java.util.Set;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.activiti.cloud.services.audit.events.QProcessEngineEventEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Applies permissions/restrictions to event data based upon property file
 */
@Component
public class SecurityPoliciesApplicationService extends BaseSecurityPoliciesApplicationService {

    @Autowired
    private SecurityPoliciesService securityPoliciesService;

    public Predicate restrictProcessEngineEventQuery(Predicate predicate,
                                                  SecurityPolicy securityPolicy) {
        if (noSecurityPoliciesOrNoUser()) {
            return predicate;
        }

        QProcessEngineEventEntity qProcessEngineEventEntity = QProcessEngineEventEntity.processEngineEventEntity;
        return buildPredicateForQProcessEngineEventEntity(predicate,
                securityPolicy,
                qProcessEngineEventEntity);
    }

    public Predicate buildPredicateForQProcessEngineEventEntity(Predicate predicate,
                                                       SecurityPolicy securityPolicy,
                                                                QProcessEngineEventEntity processEngineEventEntity) {
        BooleanExpression securityExpression = null;

        Map<String, Set<String>> restrictions = definitionKeysAllowedForPolicy(securityPolicy);

        for (String appName : restrictions.keySet()) {
            Set<String> defKeys = restrictions.get(appName);
            securityExpression = addProcessDefRestrictionToExpression(processEngineEventEntity,
                    securityExpression,
                    appName,
                    defKeys);
        }

        //policies are defined but none are applicable
        if (securityExpression == null && securityPoliciesService.policiesDefined()) {
            //user should not see anything so give unsatisfiable condition
            return getImpossiblePredicate(processEngineEventEntity);
        }

        return securityExpression != null ? securityExpression.and(predicate) : predicate;
    }

    public BooleanExpression getImpossiblePredicate(QProcessEngineEventEntity processEngineEventEntity) {
        return processEngineEventEntity.id.eq(1L).and(processEngineEventEntity.id.eq(2L));
    }

    public BooleanExpression addProcessDefRestrictionToExpression(QProcessEngineEventEntity qProcessEngineEventEntity,
                                                                  BooleanExpression securityExpression,
                                                                  String appName,
                                                                  Set<String> defKeys) {

        //expect to remove hyphens when passing in environment variables
        BooleanExpression appNamePredicate = Expressions.stringTemplate("replace({0},'-','')", qProcessEngineEventEntity.serviceName).equalsIgnoreCase(appName.replace("-",""));

        BooleanExpression nextExpression = appNamePredicate.or(Expressions.stringTemplate("replace({0},'-','')", qProcessEngineEventEntity.serviceFullName).equalsIgnoreCase(appName.replace("-","")));
        //will filter by app name and will also filter by definition keys if no wildcard
        if(!defKeys.contains(securityPoliciesService.getWildcard())){
            nextExpression = restrictByAppNameAndProcDefKeys(qProcessEngineEventEntity, defKeys, appNamePredicate);
        }

        if (securityExpression == null) {
            securityExpression = nextExpression;
        } else {
            securityExpression = securityExpression.or(nextExpression);
        }
        return securityExpression;
    }

    public BooleanExpression restrictByAppNameAndProcDefKeys(QProcessEngineEventEntity qProcessEngineEventEntity, Set<String> defKeys, BooleanExpression appNamePredicate) {
        BooleanExpression nextExpression = appNamePredicate;
        //don't actually have definitionKey in the event but do have definitionId which should contain it
        // format is e.g. SimpleProcess:version:id
        for(String key:defKeys){
            nextExpression = qProcessEngineEventEntity.processDefinitionId.startsWithIgnoreCase(key).and(nextExpression);
        }
        return nextExpression;
    }

}
