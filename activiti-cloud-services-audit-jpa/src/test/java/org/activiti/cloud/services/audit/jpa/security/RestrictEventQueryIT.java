package org.activiti.cloud.services.audit.jpa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.audit.jpa.events.ProcessStartedAuditEventEntity;
import org.activiti.cloud.services.audit.jpa.repository.EventsRepository;
import org.activiti.core.common.spring.security.policies.SecurityPolicyAccess;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Iterator;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:test-application.properties")
@SpringBootTest
@EnableAutoConfiguration
public class RestrictEventQueryIT {

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService;

    @MockBean
    private SecurityManager securityManager;

    @MockBean
    private UserGroupManager userGroupManager;


    @Test
    public void shouldGetProcessInstancesWhenPermitted() throws Exception {

        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setId(15L);
        eventEntity.setProcessDefinitionId("defKey1");
        eventEntity.setServiceName("audit");

        eventsRepository.save(eventEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }


    @Test
    public void shouldGetProcessInstancesWhenUserPermittedByWildcard() throws Exception {

        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setId(16L);
        eventEntity.setProcessDefinitionId("defKeyWild");
        eventEntity.setServiceName("audit-wild");

        eventsRepository.save(eventEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");

        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);

        assertThat(iterable.iterator().hasNext()).isTrue();
    }


    @Test
    public void shouldGetProcessInstancesWhenGroupPermittedByWildcard() throws Exception {

        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setId(17L);
        eventEntity.setProcessDefinitionId("defKeyWild");
        eventEntity.setServiceName("audit-wild");

        eventsRepository.save(eventEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("bobinhr");
        when(userGroupManager.getUserGroups("bobinhr")).thenReturn(Collections.singletonList("hrgroup"));

        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);
        assertThat(iterable.iterator().hasNext()).isTrue();
    }

    @Test
    public void shouldNotGetProcessInstancesWhenPolicyNotForUser() throws Exception {

        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setId(18L);
        eventEntity.setProcessDefinitionId("defKeyWild");
        eventEntity.setServiceName("audit-wild");

        eventsRepository.save(eventEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);

        //this user should see proc instances - but not for test-cmd-endpoint-wild

        Iterator<AuditEventEntity> iterator = iterable.iterator();
        while(iterator.hasNext()){
            AuditEventEntity auditEvent = iterator.next();
            assertThat(auditEvent.getServiceName()).isNotEqualToIgnoringCase("audit-wild");
            assertThat(auditEvent.getServiceName()).isEqualToIgnoringCase("audit");
        }
    }

    @Test
    public void shouldMatchAppNameCaseInsensitiveIgnoringHyphens() throws Exception {

        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setId(19L);
        eventEntity.setProcessDefinitionId("defKey1");
        eventEntity.setServiceName("A-uD-iT");

        eventsRepository.save(eventEntity);

        ProcessStartedAuditEventEntity eventEntity2 = new ProcessStartedAuditEventEntity();
        eventEntity2.setId(20L);
        eventEntity2.setProcessDefinitionId("defKey1");
        eventEntity2.setServiceName("audit-dontmatchthisone");

        eventsRepository.save(eventEntity2);

        assertThat(eventsRepository.count()).isGreaterThanOrEqualTo(2);

        when(securityManager.getAuthenticatedUserId()).thenReturn("testuser");

        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);

        Iterator<AuditEventEntity> iterator = iterable.iterator();
        while(iterator.hasNext()){
            AuditEventEntity auditEventEntity = iterator.next();
            assertThat(auditEventEntity.getServiceName()).isNotEqualToIgnoringCase("audit-dontmatchthisone");
            assertThat(auditEventEntity.getServiceName().replace("-","")).isEqualToIgnoringCase("audit".replace("-",""));
        }

        assertThat(eventsRepository.count(spec)).isEqualTo(2);
    }

    @Test
    public void shouldNotGetProcessInstancesWhenNotPermitted() throws Exception {

        when(securityManager.getAuthenticatedUserId()).thenReturn("intruder");

        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);
        assertThat(iterable.iterator().hasNext()).isFalse();
    }


    @Test
    public void shouldGetProcessInstancesWhenMatchesFullServiceName() throws Exception {

        ProcessStartedAuditEventEntity eventEntity = new ProcessStartedAuditEventEntity();
        eventEntity.setId(21L);
        eventEntity.setProcessDefinitionId("defKey2");
        eventEntity.setServiceName("audit");

        eventsRepository.save(eventEntity);

        when(securityManager.getAuthenticatedUserId()).thenReturn("hruser");
        Specification<AuditEventEntity> spec = securityPoliciesApplicationService.createSpecWithSecurity(null,
                SecurityPolicyAccess.READ);

        Iterable<AuditEventEntity> iterable = eventsRepository.findAll(spec);

        assertThat(iterable.iterator().hasNext()).isTrue();
    }
}
