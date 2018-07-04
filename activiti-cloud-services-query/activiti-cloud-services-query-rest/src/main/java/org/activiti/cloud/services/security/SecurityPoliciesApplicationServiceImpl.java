package org.activiti.cloud.services.security;

import java.util.Map;
import java.util.Set;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.activiti.cloud.services.query.model.QProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QVariableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Applies permissions/restrictions to ProcessInstanceEntity data (and Proc Inst Variables) based upon property file
 */
@Component
public class SecurityPoliciesApplicationServiceImpl extends BaseSecurityPoliciesApplicationService {

    @Autowired
    private SecurityPoliciesService securityPoliciesService;

    public Predicate restrictProcessInstanceQuery(Predicate predicate,
                                                  SecurityPolicy securityPolicy) {
        if (noSecurityPoliciesOrNoUser()) {
            return predicate;
        }

        QProcessInstanceEntity processInstance = QProcessInstanceEntity.processInstanceEntity;
        return buildPredicateForQProcessInstance(predicate,
                                                 securityPolicy,
                                                 processInstance);
    }

    public Predicate restrictProcessInstanceVariableQuery(Predicate predicate,
                                                          SecurityPolicy securityPolicy) {
        if (noSecurityPoliciesOrNoUser()) {
            return predicate;
        }

        QProcessInstanceEntity processInstance = QVariableEntity.variableEntity.processInstance;

        BooleanExpression varIsProcInstVar = processInstance.isNotNull();

        Predicate extendedPredicate = varIsProcInstVar;
        if (predicate != null) {
            extendedPredicate = varIsProcInstVar.and(predicate);
        }

        return buildPredicateForQProcessInstance(extendedPredicate,
                                                 securityPolicy,
                                                 processInstance);
    }

    public Predicate buildPredicateForQProcessInstance(Predicate predicate,
                                                       SecurityPolicy securityPolicy,
                                                       QProcessInstanceEntity processInstance) {
        BooleanExpression securityExpression = null;

        Map<String, Set<String>> restrictions = definitionKeysAllowedForPolicy(securityPolicy);

        for (String appName : restrictions.keySet()) {
            Set<String> defKeys = restrictions.get(appName);
            securityExpression = addProcessDefRestrictionToExpression(processInstance,
                                                                      securityExpression,
                                                                      appName,
                                                                      defKeys);
        }

        //policies are defined but none are applicable
        if (securityExpression == null && securityPoliciesService.policiesDefined()) {
            //user should not see anything so give unsatisfiable condition
            return getImpossiblePredicate(processInstance);
        }

        return securityExpression != null ? securityExpression.and(predicate) : predicate;
    }

    public BooleanExpression getImpossiblePredicate(QProcessInstanceEntity processInstance) {
        return processInstance.id.eq("1").and(processInstance.id.eq("2"));
    }

    public BooleanExpression addProcessDefRestrictionToExpression(QProcessInstanceEntity processInstance,
                                                                  BooleanExpression securityExpression,
                                                                  String appName,
                                                                  Set<String> defKeys) {

        //expect to remove hyphens when passing in environment variables
        BooleanExpression appNamePredicate = Expressions.stringTemplate("replace({0},'-','')",
                                                                        processInstance.serviceName).equalsIgnoreCase(appName.replace("-",
                                                                                                                                      ""));
        appNamePredicate = appNamePredicate.or(Expressions.stringTemplate("replace({0},'-','')",
                                                                          processInstance.serviceFullName).equalsIgnoreCase(appName.replace("-",
                                                                                                                                            "")));

        BooleanExpression nextExpression = appNamePredicate;
        //will filter by app name and will also filter by definition keys if no wildcard
        if (!defKeys.contains(securityPoliciesService.getWildcard())) {
            nextExpression = restrictByAppNameAndProcDefKeys(processInstance,
                                                             defKeys,
                                                             appNamePredicate);
        }

        if (securityExpression == null) {
            securityExpression = nextExpression;
        } else {
            securityExpression = securityExpression.or(nextExpression);
        }
        return securityExpression;
    }

    public BooleanExpression restrictByAppNameAndProcDefKeys(QProcessInstanceEntity processInstance,
                                                             Set<String> defKeys,
                                                             BooleanExpression appNamePredicate) {
        BooleanExpression nextExpression;
        nextExpression = processInstance.processDefinitionKey.in(defKeys).and(appNamePredicate);
        return nextExpression;
    }
}
