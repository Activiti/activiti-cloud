package org.activiti.cloud.security.authorization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import org.activiti.cloud.services.common.security.jwt.JwtAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootTest(
    classes = { AuthorizationTestController.class, SecurityTestConfiguration.class },
    properties = {
        "authorizations.security-constraints[0].authRoles[0]=DUMMY_ROLE",
        "authorizations.security-constraints[0].securityCollections[0].patterns[0]=/role/*",
        "authorizations.security-constraints[1].authPermissions[0]=DUMMY_PERMISSION",
        "authorizations.security-constraints[1].securityCollections[0].patterns[0]=/permission/*",
        "authorizations.security-constraints[2].securityCollections[0].patterns[0]=/public/*",
        "authorizations.security-constraints[3].authPermissions[0]=DUMMY_PERMISSION",
        "authorizations.security-constraints[3].securityCollections[0].patterns[0]=/dummy-endpoint/*",
        "authorizations.security-constraints[3].securityCollections[0].omittedMethods[0]=POST",
    }
)
@EnableWebMvc
@AutoConfigureMockMvc
public class AuthorizationConfigurerIT {

    public static final String DUMMY_BEARER = "Bearer dummy";
    public static final String AUTH_HEADER_NAME = "Authorization";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtAdapter jwtAdapterMock;

    @Autowired
    private JwtDecoder jwtDecoderMock;

    private DefaultMockMvcBuilder mockMvcBuilder;

    @BeforeEach
    void setUp() {
        mockMvcBuilder = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity());
    }

    @Test
    void should_returnOk_whenEndpointIsPublic() throws Exception {
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isOk()).build();
        mockMvc.perform(get(AuthorizationTestController.PUBLIC_GET));
        mockMvc.perform(post(AuthorizationTestController.PUBLIC_POST));
        mockMvc.perform(put(AuthorizationTestController.PUBLIC_PUT));
        mockMvc.perform(delete(AuthorizationTestController.PUBLIC_DELETE));
    }

    @Test
    void should_denyAccess_whenAuthHeaderIsNotPresent() throws Exception {
        MockMvc mockMvc = mockMvcBuilder.build();
        mockMvc.perform(get(AuthorizationTestController.ROLE_GET)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(AuthorizationTestController.PERMISSION_GET)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(AuthorizationTestController.PERMISSION_POST)).andExpect(status().isForbidden());
        mockMvc.perform(post(AuthorizationTestController.ROLE_POST)).andExpect(status().isForbidden());
        mockMvc.perform(put(AuthorizationTestController.ROLE_PUT)).andExpect(status().isForbidden());
        mockMvc.perform(delete(AuthorizationTestController.ROLE_DELETE)).andExpect(status().isForbidden());
        mockMvc.perform(put(AuthorizationTestController.PERMISSION_PUT)).andExpect(status().isForbidden());
        mockMvc.perform(delete(AuthorizationTestController.PERMISSION_DELETE)).andExpect(status().isForbidden());
    }

    @Test
    void should_return401_whenJwtIsInvalid() throws Exception {
        when(jwtDecoderMock.decode(any())).thenThrow(new InvalidBearerTokenException(""));
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isUnauthorized()).build();
        performRoleRestrictedRequests(mockMvc);
        performPermissionRestrictedRequests(mockMvc);
    }

    @Test
    void should_return403_whenJwtContainsNoRoles() throws Exception {
        when(jwtAdapterMock.getRoles()).thenReturn(Collections.emptyList());
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isForbidden()).build();
        performRoleRestrictedRequests(mockMvc);
    }

    @Test
    void should_return403_whenJwtContainsWrongRole() throws Exception {
        when(jwtAdapterMock.getRoles()).thenReturn(List.of("WRONG_ROLE"));
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isForbidden()).build();
        performRoleRestrictedRequests(mockMvc);
    }

    @Test
    void should_return200_whenJwtContainsCorrectRole() throws Exception {
        when(jwtAdapterMock.getRoles()).thenReturn(List.of("DUMMY_ROLE"));
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isOk()).build();
        performRoleRestrictedRequests(mockMvc);
    }

    @Test
    void should_return403_whenJwtContainsNoPermissions() throws Exception {
        when(jwtAdapterMock.getPermissions()).thenReturn(Collections.emptyList());
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isForbidden()).build();
        performPermissionRestrictedRequests(mockMvc);
    }

    @Test
    void should_return403_whenJwtContainsWrongPermission() throws Exception {
        when(jwtAdapterMock.getPermissions()).thenReturn(List.of("WRONG_PERMISSION"));
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isForbidden()).build();
        performPermissionRestrictedRequests(mockMvc);
    }

    @Test
    void should_return200_whenJwtContainsCorrectPermission() throws Exception {
        when(jwtAdapterMock.getPermissions()).thenReturn(List.of("DUMMY_PERMISSION"));
        MockMvc mockMvc = mockMvcBuilder.alwaysExpect(status().isOk()).build();
        performPermissionRestrictedRequests(mockMvc);
    }

    @Test
    void should_return405_whenJwtContainsCorrectPermissionButMethodIsOmitted() throws Exception {
        MockMvc mockMvc = mockMvcBuilder.build();
        when(jwtAdapterMock.getPermissions()).thenReturn(List.of("DUMMY_PERMISSION"));
        mockMvc
            .perform(get(AuthorizationTestController.DUMMY_ENDPOINT).header(AUTH_HEADER_NAME, DUMMY_BEARER))
            .andExpect(status().isOk());
        mockMvc
            .perform(post(AuthorizationTestController.DUMMY_ENDPOINT).header(AUTH_HEADER_NAME, DUMMY_BEARER))
            .andExpect(status().isForbidden());
    }

    private void performRoleRestrictedRequests(MockMvc mockMvc) throws Exception {
        mockMvc.perform(get(AuthorizationTestController.ROLE_GET).header(AUTH_HEADER_NAME, DUMMY_BEARER));
        mockMvc.perform(post(AuthorizationTestController.ROLE_POST).header(AUTH_HEADER_NAME, DUMMY_BEARER));
        mockMvc.perform(put(AuthorizationTestController.ROLE_PUT).header(AUTH_HEADER_NAME, DUMMY_BEARER));
        mockMvc.perform(delete(AuthorizationTestController.ROLE_DELETE).header(AUTH_HEADER_NAME, DUMMY_BEARER));
    }

    private void performPermissionRestrictedRequests(MockMvc mockMvc) throws Exception {
        mockMvc.perform(get(AuthorizationTestController.PERMISSION_GET).header(AUTH_HEADER_NAME, DUMMY_BEARER));
        mockMvc.perform(post(AuthorizationTestController.PERMISSION_POST).header(AUTH_HEADER_NAME, DUMMY_BEARER));
        mockMvc.perform(put(AuthorizationTestController.PERMISSION_PUT).header(AUTH_HEADER_NAME, DUMMY_BEARER));
        mockMvc.perform(delete(AuthorizationTestController.PERMISSION_DELETE).header(AUTH_HEADER_NAME, DUMMY_BEARER));
    }
}
