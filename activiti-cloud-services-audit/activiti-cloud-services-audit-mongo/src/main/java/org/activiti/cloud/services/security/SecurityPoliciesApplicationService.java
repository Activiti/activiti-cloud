package org.activiti.cloud.services.security;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.activiti.cloud.services.audit.mongo.events.QProcessEngineEventDocument;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies permissions/restrictions to event data based upon property file
 */
@Component
public class SecurityPoliciesApplicationService {

    @Autowired(required = false)
    private UserGroupLookupProxy userGroupLookupProxy;

    @Autowired(required = false)
    private UserRoleLookupProxy userRoleLookupProxy;

    @Autowired
    private AuthenticationWrapper authenticationWrapper;

    @Autowired
    private SecurityPoliciesService securityPoliciesService;

    public Predicate restrictProcessEngineEventQuery(Predicate predicate,
                                                  SecurityPolicy securityPolicy) {
        if (noSecurityPoliciesOrNoUser()) {
            return predicate;
        }

        QProcessEngineEventDocument qProcessEngineEventEntity = QProcessEngineEventDocument.processEngineEventDocument;
        return buildPredicateForQProcessEngineEventEntity(predicate,
                securityPolicy,
                qProcessEngineEventEntity);
    }

    public Predicate buildPredicateForQProcessEngineEventEntity(Predicate predicate,
                                                       SecurityPolicy securityPolicy,
                                                                QProcessEngineEventDocument processEngineEventEntity) {
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

    public BooleanExpression getImpossiblePredicate(QProcessEngineEventDocument processEngineEventEntity) {
        return processEngineEventEntity.id.eq("1").and(processEngineEventEntity.id.eq("2"));
    }

    public BooleanExpression addProcessDefRestrictionToExpression(QProcessEngineEventDocument qProcessEngineEventEntity,
                                                                  BooleanExpression securityExpression,
                                                                  String appName,
                                                                  Set<String> defKeys) {

        //expect to remove hyphens when passing in environment variables
        BooleanExpression appNamePredicate = Expressions.stringTemplate("replace({0},'-','')", qProcessEngineEventEntity.applicationName).equalsIgnoreCase(appName.replace("-",""));

        BooleanExpression nextExpression = appNamePredicate;
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

    public BooleanExpression restrictByAppNameAndProcDefKeys(QProcessEngineEventDocument qProcessEngineEventEntity, Set<String> defKeys, BooleanExpression appNamePredicate) {
        BooleanExpression nextExpression = appNamePredicate;
        //don't actually have definitionKey in the event but do have definitionId which should contain it
        // format is e.g. SimpleProcess:version:id
        for(String key:defKeys){
            nextExpression = qProcessEngineEventEntity.processDefinitionId.startsWithIgnoreCase(key).and(nextExpression);
        }
        return nextExpression;
    }

    private boolean noSecurityPoliciesOrNoUser() {
        return !securityPoliciesService.policiesDefined() || authenticationWrapper.getAuthenticatedUserId() == null;
    }

    private Map<String, Set<String>> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;

        if (userGroupLookupProxy != null && authenticationWrapper.getAuthenticatedUserId() != null) {
            groups = userGroupLookupProxy.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPoliciesService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups,
                securityPolicy);
    }

    public boolean canRead(String processDefId,
                           String appName) {
        return hasPermission(processDefId,
                SecurityPolicy.READ,
                appName);
    }

    private boolean hasPermission(String processDefId,
                                  SecurityPolicy securityPolicy,
                                  String appName) {

        if (!securityPoliciesService.policiesDefined() || userGroupLookupProxy == null || authenticationWrapper.getAuthenticatedUserId() == null) {
            return true;
        }

        if (userRoleLookupProxy != null && userRoleLookupProxy.isAdmin(authenticationWrapper.getAuthenticatedUserId())) {
            return true;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy).get(appName);

        return (keys != null && (anEntryInSetStartsId(keys,processDefId) || keys.contains(securityPoliciesService.getWildcard())));
    }

    private boolean anEntryInSetStartsId(Set<String> keys,String processDefId){
        for(String key:keys){
            if(processDefId.startsWith(key)){
                return true;
            }
        }
        return false;
    }
}
