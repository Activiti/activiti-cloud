package org.activiti.cloud.services.identity.basic;

import org.activiti.runtime.api.auth.AuthorizationLookup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BasicAuthorizationLookupIT {

    @Autowired
    private AuthorizationLookup authorizationLookup;

    @org.springframework.context.annotation.Configuration
    @ComponentScan("org.activiti.cloud.services.identity.basic")
    public static class Configuration {

    }

    @Test
    public void testAdminRole() throws Exception {
        assertThat(authorizationLookup.isAdmin("client")).isTrue();
        assertThat(authorizationLookup.isAdmin("testuser")).isFalse();
    }
}
